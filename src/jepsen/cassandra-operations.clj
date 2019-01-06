(in-ns 'jepsen.cassandra)


(def closeConnection (fn [conn]    (SeatsClient/closeConnection conn)))
(def openConnection  (fn [ip]      (SeatsClient/getConnection ip)))

(def operationMap [{:n 1, :f :DR-TXN, 
                          :javaFunc (fn [conn args] (SeatsClient/testTxn conn (nth args 0))), 
                          :freq 100/100},

                   ])


;====================================================================================================
; generate a new flight id
(defn gen_flight
  [index]
  {:f_id (SeatsUtils/getExistingResFlightId index)})

; generate a new customer id
(defn gen_cust
  [canBeNull, index]
  (if (and canBeNull (< (rand) consts/_CUST_BY_STR_PROB))
      ; generates a customer with null (=-1) id with a string containing the id
      {:c_id -1,
       :c_id_str (str (SeatsUtils/getExistingResCustomerId index))}
      ; generates a customer with a valid id
      {:c_id (SeatsUtils/getExistingResCustomerId index),
        :c_id_str ""}))

; generate a new airline id
(defn gen_al
  []
  {:al_id (SeatsUtils/getNextAirlineId)})

; generate a new frequent flyer id
(defn gen_ff
  []
  (if (< (rand) consts/_FF_SHOULD_BE_UPDATED)
  {:ff_c_id_str (str (:c_id (gen_cust false (SeatsUtils/getRandomResIndex)))),
   :ff_al_id (:al_id (gen_al))}
  {:ff_c_id_str (str -1),:ff_al_id -1}))
;
;
;
(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
    ;deleteReservation
    (let [index (SeatsUtils/getRandomResIndex)
            cust (gen_cust false index) 
            ]
      [(:c_id cust)])
    )

;====================================================================================================
(defn errorMap
  [errorCode]
  (condp = errorCode 
    -1 "Generic exception thrown."
     1 "No Customer information based on the given c_id_str found."
       "unknown error.")
  )


