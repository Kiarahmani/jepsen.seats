(defproject jepsen.cassandra "0.1.0-SNAPSHOT"
  :description "A Jepsen test for Cassandra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main jepsen.cassandra
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [jepsen "0.1.11"]
		 [environ "1.1.0"]
		 [verschlimmbesserung "0.1.3"]]
  :java-source-paths ["/home/ubuntu/Jepsen_Java_Tests/src/main/java"]
  :resource-paths ["resources/cassandra-jdbc-wrapper-3.1.0-SNAPSHOT.jar"]
  :jvm-opts ["-Xms13g" "-Xmx13g"] 
)
