(in-ns 'jepsen.cassandra)


; THE ORIGINAL FILE: benchmarks/kv.clj
;XXX THIS FILE IS COPIED INTO cassandra-operations.clj AT RUNTIME



(def closeConnection (fn [conn]    (utils.CassConn/closeConnection conn)))
(def openConnection  (fn [ip]      (utils.CassConn/getConnection ip)))

(def operationMap [{:n 1, :f :NO-TXN,
                          :javaFunc (fn [conn args] (tpcc.NewOrder/newOrder conn)),
                          :freq 100/100}
                   ])






;====================================================================================================

;
;

(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
  (condp = txnNo
    ;withdraw
    1 []
    (info "ERROR!! ---> UNKNOWN txnNo")
    ))





;====================================================================================================
(defn errorMap
  [errorCode]
  (condp = errorCode
    -1 "Generic exception thrown."
    1  "An account has negative balance"
     "unknown error.")
  )


