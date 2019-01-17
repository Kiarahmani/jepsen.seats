(in-ns 'jepsen.cassandra)


; THE ORIGINAL FILE: benchmarks/seats.clj
;XXX THIS FILE IS COPIED INTO cassandra-operations.clj AT RUNTIME


(def operationMap [{:n 1, :f :DR-TXN, 
                          :javaFunc (fn [conn args] (seats.DeleteReservation/deleteReservation conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4))), 
                          :freq 10/100},;10
                   {:n 2, :f :FF-TXN, 
                          :javaFunc (fn [conn args] (seats.FindFlights/findFlights conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4))), 
                          :freq 10/100},;10
                   {:n 3, :f :FOS-TXN, 
                          :javaFunc (fn [conn args] (seats.FindOpenSeats/findOpenSeats conn (nth args 0))), 
                          :freq 35/100},;35
                   {:n 4, :f :NR-TXN, 
                          :javaFunc (fn [conn args] (seats.NewReservation/newReservation conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5))), 
                          :freq 20/100},;20
                   {:n 5, :f :UC-TXN, 
                          :javaFunc (fn [conn args] (seats.UpdateCustomer/updateCustomer conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4))), 
                          :freq 10/100},;10
                   {:n 6, :f :UR-TXN, 
                          :javaFunc (fn [conn args] (seats.UpdateReservation/updateReservation conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5))), 
                          :freq 15/100};15

                   ])







;====================================================================================================
; generate a new flight id
(defn gen_flight
  [index]
  {:f_id (seats.Utils_seats/getExistingResFlightId index)})

; generate a new customer id
(defn gen_cust
  [canBeNull, index]
  (if (and canBeNull (< (rand) consts/_CUST_BY_STR_PROB))
      ; generates a customer with null (=-1) id with a string containing the id
      {:c_id -1,
       :c_id_str (str (seats.Utils_seats/getExistingResCustomerId index))}
      ; generates a customer with a valid id
      {:c_id (seats.Utils_seats/getExistingResCustomerId index),
        :c_id_str ""}))

; generate a new airline id
(defn gen_al
  []
  {:al_id (seats.Utils_seats/getNextAirlineId)})

; generate a new frequent flyer id
(defn gen_ff
  []
  (if (< (rand) consts/_FF_SHOULD_BE_UPDATED)
  {:ff_c_id_str (str (:c_id (gen_cust false (seats.Utils_seats/getRandomResIndex)))),
   :ff_al_id (:al_id (gen_al))}
  {:ff_c_id_str (str -1),:ff_al_id -1}))
;
;
;

(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
  (condp = txnNo
    ;deleteReservation
    1 (let [index (seats.Utils_seats/getRandomResIndex)
            cust (gen_cust true index) 
            ff   (gen_ff)
            f    (gen_flight index)]
      [(:f_id f),(:c_id cust), (:c_id_str cust), (:ff_c_id_str ff), (:ff_al_id ff)])
    ;FindFlights
    2 (let [beginDate (seats.Utils_seats/getNextRandomDate)] 
      [(rand-int consts/_AIRPORT_COUNT),(rand-int consts/_AIRPORT_COUNT),beginDate,(seats.Utils_seats/getNextDateWithBegin beginDate),(rand-int consts/_MAXIMUM_ACCEPTABLE_DISTANCE)])
    ;FindOpenSeats
    3  [(let [index (seats.Utils_seats/getRandomResIndex)
              f (gen_flight index)]
        (:f_id f))]
    ;NewReservation
    4  (let [index   (seats.Utils_seats/getRandomResIndex)
             custIDX (seats.Utils_seats/getRandomResIndex)
             r_id    (seats.Utils_seats/getNewResId)
             f       (gen_flight index)
             c       (gen_cust false custIDX)
             seatnum (+ (rand-int 146) 3)
             attrs   (seats.Utils_seats/getNewAttrs)]
        [r_id,(:c_id c),(:f_id f),seatnum,1,attrs])
    ;UpdateCustomer
    5  (let [index (seats.Utils_seats/getRandomResIndex)
             cust (gen_cust true index)]
       [(:c_id cust), (:c_id_str cust),(- (rand-int 5) 2),(long (rand index)),(long (rand index))])
    ;UpdateReservation
    6  (let [index (seats.Utils_seats/getRandomResIndex)
             r_id    (seats.Utils_seats/getNewResId)
             c (gen_cust false index)
             f (gen_flight index)
             seatnum (+ (rand-int 147) 2)]
        [r_id,(:f_id f),(:c_id c),seatnum,1,(long (rand (seats.Utils_seats/getRandomResIndex)))])
    (info "ERROR!! ---> UNKNOWN txnNo")
    ))





;====================================================================================================
(defn errorMap
  [errorCode]
  (condp = errorCode 
    -1 "Generic exception thrown."
    
    ; delete reservation
    11 "No Customer information based on the given c_id_str found."
    12 "c_i (either given or extracted based on customer name) does not exist"
    13 "given f_id does not exit"
    14 "reservation for the given/extracted f_id and c_id does not exist"
    15 "delete did NOT succeed" 
    16 "update flight did NOT succeed"
    17 "update customer balance did NOT succeed"
    18 "Frequent Flyer does not exist"
    19 "update frequent flyer did NOT succeed"
    
    ; find flights
    21 "requested airline does not exist"
    22 "departure airport does not exist"
    23 "arrival airport does not exist"
  
    ; find open seats
    31 "given f_id does not exist"
    32 "duplicate seat reservations"
    
    ; new reservation
    41 "invalid f_id is given"
    42 "extracted airline_id is invalid"
    43 "No more seats available for flight"
    44 "Seatis already reserved on flight"
    45 "Customer already owns on a reservations on flight"
    46 "Invalid customer id"


    ; update customer
    51 "No Customer information record found for the string"
    52 "No Customer information record found for the given c_id"
    53 "unacceptable state: wrong customer is retrieved"
    54 "base airport_id is invalid"
    55 "country does not exist"

    ; update reservation
    61 "Seat is already reserved on flight"
    62 "Customer does not have an existing reservation on flight"

     "unknown error.")
  )


