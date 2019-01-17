(in-ns 'jepsen.cassandra)
; XXX
; THE ORIGINAL FILE: benchmarks/kv.clj
; THE ORIGINAL FILE WILL BE COPIED INTO cassandra-operations.clj AT RUNTIME
; 




(def operationMap [{:n 1, :f :NO-TXN,
                          :javaFunc (fn [conn args] (tpcc.NewOrder/newOrder conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5)(nth args 6)(nth args 7))),
                          :freq 45/100}
                   {:n 2, :f :PM-TXN,
                          :javaFunc (fn [conn args] (tpcc.Payment/payment conn)),
                          :freq 43/100}
                   {:n 3, :f :OS-TXN,
                          :javaFunc (fn [conn args] (tpcc.OrderStatus/orderStatus conn)),
                          :freq 4/100}
                   {:n 4, :f :DV-TXN,
                          :javaFunc (fn [conn args] (tpcc.Delivery/delivery conn)),
                          :freq 4/100}
                   {:n 5, :f :SL-TXN,
                          :javaFunc (fn [conn args] (tpcc.StockLevel/stockLevel conn)),
                          :freq 4/100}])
;====================================================================================================
;

(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
  (condp = txnNo
    1 [1,1,1,20,30,nil,nil,nil]
    2 []
    3 []
    4 []
    5 []
    (info "ERROR!! ---> UNKNOWN txnNo")))


;====================================================================================================
(defn errorMap
  [errorCode]
  (condp = errorCode
    -1 "Generic exception thrown."
    1  "An account has negative balance"
     "unknown error.")
  )


