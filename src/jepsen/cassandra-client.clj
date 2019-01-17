(in-ns 'jepsen.cassandra)

;; CLIENT
;;====================================================================================
(defrecord Client [conn]
  client/Client
  (open! [this test node]
	(assoc this :conn (openConnection (dns-resolve node))))    
  (setup! [this test]
    (info ">>> Initializing tests: setting up data structures")
    (let [bench (:bench test)
          scale (:scale test)]
      (clojure.lang.Reflector/invokeConstructor
        (resolve (symbol (str bench ".Utils_" bench))) (object-array [(int scale)]))))

  (invoke! [this test op]
      (let [txn (:javaFunc (first (filter (fn [m] (= (:f m) (:f op))) operationMap)))
            retStatus (txn conn (:args op))]  
                  (assoc op :type :ok, :returnStatus retStatus, :value retStatus)))
        
  (teardown! [this test]
	(closeConnection conn))
  (close! [_ test]))


