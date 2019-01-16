(in-ns 'jepsen.cassandra)


; THE ORIGINAL FILE: benchmarks/kv.clj
;XXX THIS FILE IS COPIED INTO cassandra-operations.clj AT RUNTIME



(def closeConnection (fn [conn]    (utils.CassConn/closeConnection conn)))
(def openConnection  (fn [ip]      (utils.CassConn/getConnection ip)))

(def operationMap [{:n 1, :f :CHCK-TXN,
                          :javaFunc (fn [conn args] (kv.DepositChecking/deposit_checking conn (nth args 0)(nth args 1))),
                          :freq 45/100},
                   {:n 2, :f :SVNG-TXN,
                          :javaFunc (fn [conn args] (kv.DepositSaving/deposit_saving conn (nth args 0)(nth args 1))),
                          :freq 45/100}
                   {:n 3, :f :TEST-TXN,
                          :javaFunc (fn [conn args] (kv.Check/check conn (nth args 0))),
                          :freq 10/100}
                   ])






;====================================================================================================
; generate a new flight id


(defn gen_bal
  []
  (rand-int 20))

(defn gen_id
  []
  (+ (rand-int 999) 1)
  )
;
;

(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
  (condp = txnNo
    ;withdraw
    1 [(gen_id), (gen_bal)]
    2 [(gen_id), (gen_bal)]
    3 [(gen_id)]
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


