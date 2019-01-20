(in-ns 'jepsen.cassandra)
; XXX
; THE ORIGINAL FILE: benchmarks/kv.clj
; THE ORIGINAL FILE WILL BE COPIED INTO cassandra-operations.clj AT RUNTIME
; 




(def operationMap [{:n 1, :f :NO-TXN,
                          :javaFunc (fn [conn args] (tpcc.NewOrder/newOrder conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5)(nth args 6)(nth args 7))),
                          :freq 45/100}
                   {:n 2, :f :PM-TXN,
                          :javaFunc (fn [conn args] (tpcc.Payment/payment conn   (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5)(nth args 6)(nth args 7))),
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
    1 (let [w_id      (tpcc.Utils_tpcc/get_w_id)
            d_id      (tpcc.Utils_tpcc/get_d_id)
            c_id      (tpcc.Utils_tpcc/get_c_id)
            num_items (tpcc.Utils_tpcc/get_num_items)
            wh_sup_and_lcl_list (tpcc.Utils_tpcc/get_sup_wh_and_o_all_local num_items w_id)
            wh_sup    (nth wh_sup_and_lcl_list 0)
            all_local (nth wh_sup_and_lcl_list 1)
            item_ids  (tpcc.Utils_tpcc/get_item_ids num_items)
            order_qnts(tpcc.Utils_tpcc/get_order_quantities num_items)]
        [w_id,d_id,c_id,all_local,num_items,item_ids,wh_sup,order_qnts])

    2 (let [w_id                (tpcc.Utils_tpcc/get_w_id)
            d_id                (tpcc.Utils_tpcc/get_d_id)
            payment_cust        (tpcc.Utils_tpcc/get_payment_cust)
            customerByName      (nth payment_cust 0)
            c_id                (nth payment_cust 1)
            c_last              (nth payment_cust 2)
            cust_info           (tpcc.Utils_tpcc/get_customerinfo w_id d_id)
            customerWarehouseID (nth cust_info 0)
            customerDistrictID  (nth cust_info 1)
            paymentAmount       (tpcc.Utils_tpcc/get_paymentAmount)]
        [w_id,d_id,customerByName,c_id,c_last,customerWarehouseID,customerDistrictID,paymentAmount])
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


