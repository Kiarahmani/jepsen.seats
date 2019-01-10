(in-ns 'jepsen.cassandra)

;; UTILS
;;====================================================================================
(defn cached-install?
  [src]
  (try (c/exec :grep :-s :-F :-x (lit src) (lit ".download"))
       true
       (catch RuntimeException _ false)))
;
(defn disable-hints?
  "Returns true if Jepsen tests should run without hints"
  []
  (not (System/getenv "JEPSEN_DISABLE_HINTS")))
;
(defn phi-level
  "Returns the value to use for phi in the failure detector"
  []
  (or (System/getenv "JEPSEN_PHI_VALUE")
      8))
;
(defn coordinator-batchlog-disabled?
  "Returns whether to disable the coordinator batchlog for MV"
  []
  (boolean (System/getenv "JEPSEN_DISABLE_COORDINATOR_BATCHLOG")))
;
(defn compressed-commitlog?
  "Returns whether to use commitlog compression"
  []
  (= (some-> (System/getenv "JEPSEN_COMMITLOG_COMPRESSION") (clojure.string/lower-case))
     "true"))
;


(defn dns-resolve
  "Gets the address of a hostname"
  [hostname]
  (.getHostAddress (InetAddress/getByName (name hostname))))

(def dir     "~/cassandra/logs")
(def logfile (str dir "/system.log"))


;; CONFIGURATION PROCEDURES
;;====================================================================================
(defn initJava!
	"Installs Java on the given node"
	[node version]
	(do 
	(info node "Installing Java...")
	(c/su
   	(c/cd
	(c/exec :echo
     		"deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main"
     		:>"/etc/apt/sources.list.d/webupd8team-java.list")
    	(c/exec :echo
     		"deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main"
     		:>> "/etc/apt/sources.list.d/webupd8team-java.list")
    		(try (c/exec :apt-key :adv :--keyserver "hkp://keyserver.ubuntu.com:80"
              		:--recv-keys "EEA14886")
        		(debian/update!)
        	 	(catch RuntimeException e
           			(info "Error updating caused by" e)))
    		(c/exec :echo
            		"debconf shared/accepted-oracle-license-v1-1 select true"
            		| :debconf-set-selections)
    		(debian/install [:oracle-java8-installer])))))

;;====================================================================================
(defn install!
	"Installs Cassandra on the given node"
	[node version]
	(info ">>>>>> starting cassandra installation...")
 (c/su
   (c/cd
    "/tmp"
    (let [tpath (System/getenv "CASSANDRA_TARBALL_PATH")
          url (or tpath
                  (System/getenv "CASSANDRA_TARBALL_URL")
                  (str "http://apache.claz.org/cassandra/" version
                       "/apache-cassandra-" version "-bin.tar.gz"))]
      (info ">>>>>> installing Cassandra from" url)
      (if (cached-install? url)
        (info ">>>>>> used cached install on node" node)
        (do (if tpath ;; else: if not found locally, download it
              (c/upload tpath "/tmp/cassandra.tar.gz")
              (c/exec :wget :-O "cassandra.tar.gz" url (lit ";")))
            (c/exec :tar :xzvf "cassandra.tar.gz" :-C "~")
            (c/exec :rm :-r :-f (lit "~/cassandra"))
            (c/exec :mv (lit "~/apache* ~/cassandra"))
            (c/exec :echo url :> (lit ".download"))))))))


(defn allNodes!
  "given the current test setup, will return all node names separated with commas to be used in Cassandra configurations"
  [test]
  (let [nodeList   (:nodes test)
        nodeString (reduce (fn [oldN n] 
                             (if (= oldN "")
                             (str oldN n)
                             (str oldN "," n))) "" nodeList)]
       (str "'" nodeString "'")))
  

;;====================================================================================
(defn configure!
  "Uploads configuration files to the given node."
  [node test]
  (info node (str "configuring Cassandra"))
  (c/su
 	(doseq [rep [
                     ;"\"s/#MAX_HEAP_SIZE=.*/MAX_HEAP_SIZE='512M'/g\""
                     ;"\"s/#HEAP_NEWSIZE=.*/HEAP_NEWSIZE='128M'/g\""
                     "\"s/LOCAL_JMX=yes/LOCAL_JMX=no/g\""
                (str "'s/# JVM_OPTS=\"$JVM_OPTS -Djava.rmi.server.hostname="
                     "<public name>\"/JVM_OPTS=\"$JVM_OPTS -Djava.rmi.server.hostname="
                     (name node) "\"/g'")
                (str "'s/JVM_OPTS=\"$JVM_OPTS -Dcom.sun.management.jmxremote"
                     ".authenticate=true\"/JVM_OPTS=\"$JVM_OPTS -Dcom.sun.management"
                     ".jmxremote.authenticate=false\"/g'")
                "'/JVM_OPTS=\"$JVM_OPTS -Dcassandra.mv_disable_coordinator_batchlog=.*\"/d'"]]
     (c/exec :sed :-i (lit rep) "~/cassandra/conf/cassandra-env.sh"))
     (doseq [rep (into ["\"s/cluster_name: .*/cluster_name: 'jepsen'/g\""
                      "\"s/row_cache_size_in_mb: .*/row_cache_size_in_mb: 20/g\""
		      "\"s/endpoint_snitch: .*/endpoint_snitch: GossipingPropertyFileSnitch/g\""
                      (str "\"s/seeds: .*/seeds: " (allNodes! test) "/g\"")
                      (str "\"s/listen_address: .*/listen_address: " (dns-resolve node)
                           "/g\"")
                      (str "\"s/read_request_timeout_in_ms: .*/read_request_timeout_in_ms: 10000" "/g\"")
                      (str "\"s/rpc_address: .*/rpc_address: " (dns-resolve node) "/g\"")
                      (str "\"s/broadcast_rpc_address: .*/broadcast_rpc_address: "
                           (net/local-ip) "/g\"")
                      "\"s/internode_compression: .*/internode_compression: none/g\""
                      (str "\"s/hinted_handoff_enabled:.*/hinted_handoff_enabled: "
                           (disable-hints?) "/g\"")
                      "\"s/commitlog_sync: .*/commitlog_sync: batch/g\""
                      (str "\"s/# commitlog_sync_batch_window_in_ms: .*/"
                           "commitlog_sync_batch_window_in_ms: 1.0/g\"")
                      "\"s/commitlog_sync_period_in_ms: .*/#/g\""
                      (str "\"s/# phi_convict_threshold: .*/phi_convict_threshold: " (phi-level)
                           "/g\"")
                       "\"/auto_bootstrap: .*/d\""]
                      (when (compressed-commitlog?)
                      ["\"s/#commitlog_compression.*/commitlog_compression:/g\""
                      (str "\"s/#   - class_name: LZ4Compressor/"
                      "    - class_name: LZ4Compressor/g\"")]))]
     		      (c/exec :sed :-i (lit rep) "~/cassandra/conf/cassandra.yaml"))
     (doseq [rep (into [(str "\"s/dc=dc1/dc=dc_" node "/g\"")
			(str "\"s/rack=rack1/rack=rack_" node "/g\"")])]
     		      (c/exec :sed :-i (lit rep) "~/cassandra/conf/cassandra-rackdc.properties"))

     (c/exec :echo (str "JVM_OPTS=\"$JVM_OPTS -Dcassandra.mv_disable_coordinator_batchlog="
                      	(coordinator-batchlog-disabled?) "\"") 
     			:>> "~/cassandra/conf/cassandra-env.sh")
     (c/exec :sed :-i (lit "\"s/INFO/DEBUG/g\"") "~/cassandra/conf/logback.xml"):while))
;;====================================================================================
; create KEYSPACE seats WITH replication = {'class': 'NetworkTopologyStrategy', 'dc_n1':1,'dc_n2':1,'dc_n3':1, 'dc_n4':1, 'dc_n5':1};

(defn prepareKS!
  "returns the string for cqlsh to make the keysapace based on the number of nodes"
  [dcs]
  (let [dropComm "drop KEYSPACE IF EXISTS seats;"
        dcString (reduce (fn [oldDc newDc] (str oldDc ", 'dc_" newDc "':1")) "" dcs)
        finalComm (str dropComm " create KEYSPACE seats WITH replication = {'class': 'NetworkTopologyStrategy'" dcString "};")]
    finalComm
    ))

(defn prepareDB! 
  "creating keyspace and tables"
  [node test tables]
  (when (= (str node) "n1") 
    (info ">>> creating keyspace on: " (dns-resolve node))
    (c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " node " -e \"" (prepareKS! (:nodes test)) "\"" ))
    (info ">>> creating tablese on: " (dns-resolve node))
    (c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/resource/ddl.cql" ))
    (info ">>> keyspace and tables intialized")
    (doseq [t tables]
      (do ; .csv solution:
          ;(info (str "Loading raw data in table: " t))
          ;(c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/resource/load_" t ".cql" ))
          
          ; snapshot based solution:
          (info (str "Copying snapshots for: " t))
          (c/exec* (str "/home/ubuntu/cassandra/bin/sstableloader  -d " (dns-resolve node) " /home/ubuntu/" consts/_KEYSPACE_NAME "/" t))
          ))
    ;(c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/load.cql" ))
    (Thread/sleep 1000)
    (info (str ">>> initial SSTables loaded ")
    (info ">>> creating a java connection pool")
    )))





;;====================================================================================
(defn start!
  "Starting Cassandra..."
  [node test]
  (info node (str "starting Cassandra on " (dns-resolve node)))
  (c/su
   (c/exec* (str "/home/ubuntu/cassandra/bin/cassandra -R"))))
;;
(defn stop!
  "Stopping Cassandra..."
  [node]
  (info node "stopping Cassandra")
  (c/su
   (meh (c/exec :killall :java))
   (while (.contains (c/exec :ps :-ef) "java")
     (Thread/sleep 100)))
  (info node "has stopped Cassandra"))
;;
(defn wipe!
  "Shuts down Cassandra and wipes data."
  [node]
  (stop! node)
  (info node "deleting data files")
  (c/su
   (meh (c/exec :rm :-r "~/cassandra/logs"))
   (meh (c/exec :rm :-r "~/cassandra/data/data"))
   (meh (c/exec :rm :-r "~/cassandra/data/hints"))
   (meh (c/exec :rm :-r "~/cassandra/data/commitlog"))
   (meh (c/exec :rm :-r "~/cassandra/data/saved_caches"))))










