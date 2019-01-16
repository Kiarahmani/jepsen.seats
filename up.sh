#!/bin/bash
# ------------------------------------------------------------------
# [Author] Kia Rahmani
#          Blackbox testing based on Jepsen on EC2
# ------------------------------------------------------------------

VERSION=0.1.0
SUBJECT=1001011101
USAGE="Usage: command -ihv args"

# --- Options processing -------------------------------------------
for i in "$@"
do
case $i in
    -c=*|--concurrency=*)
    CONCURRENCY="${i#*=}"
    shift # past argument=value
    ;;
    -r=*|--load_raw=*)
    LOADRAW="${i#*=}"
    shift # past argument=value
    ;;
    -k=*|--init_ks=*)
    KS="${i#*=}"
    shift # past argument=value
    ;;
    -b=*|--bench=*)
    BENCH="${i#*=}"
    shift # past argument=value
    ;;
    -t=*|--time=*)
    TIME="${i#*=}"
    shift # past argument=value
    ;;
    -d=*|--init_db=*)
    DB="${i#*=}"
    shift # past argument=value
    ;;
    --default)
    DEFAULT=YES
    shift # past argument with no value
    ;;
    *)
          # unknown option
    ;;
esac
done
echo "====================================="
echo "     Automated Jepsen Test on EC2"
echo "====================================="
echo "Conccurency   = ${CONCURRENCY}"
echo "Time          = ${TIME}"
echo "Benchmark     = ${BENCH}"
echo "Load Raw Data = ${LOADRAW}"
echo "Initialize DB = ${DB}"
echo "Initialize KS = ${KS}"
echo "====================================="

# --- Parameter Handling ------------------------------------------
if [ ${DB} = "true" ]; then
	DB="--init-db"
else
	DB=""
fi

if [ ${KS} = "true" ]; then
	KS="--init-ks"
else
	KS=""
fi



# --- Locks -------------------------------------------------------
LOCK_FILE=/tmp/$SUBJECT.lock
if [ -f "$LOCK_FILE" ]; then
   echo "Script is already running"
   exit
fi
trap "rm -f $LOCK_FILE" EXIT
touch $LOCK_FILE



# --- Body --------------------------------------------------------
#
echo ">>> updating java application from git repository:"
cd ~/Jepsen_Java_Tests/ 
git pull
cd - >/dev/null
echo "done."



#
echo ">>> replacing generic cassandra-operation.clj with benchmark specific file"
cp /home/ubuntu/jepsen.seats/src/jepsen/benchmarks/$BENCH.clj /home/ubuntu/jepsen.seats/src/jepsen/cassandra-operations.clj
echo "done."

# 
echo "copying table.names file from Application's dir to the config dir"
cp /home/ubuntu/Jepsen_Java_Tests/src/main/java/$BENCH/table.names /home/ubuntu/jepsen.seats/config/table.names
echo "done."


#
if [ ${LOADRAW} = "true" ]; then
	# copying necessary files to jepsen nodes
	while IFS='' read -r line || [[ -n "$line" ]]; do
	    echo ">>> copying resource files to node: $line"
	    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/src/main/java/$BENCH/table.names ubuntu@${line}:/home/ubuntu/resource/ 
	    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/src/main/java/$BENCH/ddl.cql ubuntu@${line}:/home/ubuntu/resource/ 
	    #while IFS='' read -r table || [[ -n "$table" ]]; do
	    #	scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/load_${table}.cql ubuntu@${line}:/home/ubuntu/resource/ >/dev/null
	    #done < "/home/ubuntu/table.names" 
	    echo "done."
    	done < "/home/ubuntu/jepsen.seats/config/nodes"

	echo ">>> copying snapshots to n1"
	scp -r -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/jepsen.seats/snapshots/$BENCH ubuntu@n1:/home/ubuntu/
fi




echo ""
echo ">>> calling Jepsen:"
time lein run test --nodes-file /home/ubuntu/jepsen.seats/config/nodes  --concurrency ${CONCURRENCY} --time-limit ${TIME} ${DB} ${KS} --init-java  --username ubuntu --ssh-private-key ~/.ssh/ec2-ohio.pem --bench ${BENCH}






echo ""
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo ">>> analyzing logs:"
UniqeErrors=`grep -E "TXN\s[1-9][1-9]" store/latest/history.txt | cut -f3,4 | sort | uniq`
echo
for i in $UniqeErrors; do
	if [[ $i == :* ]]; then
		echo -n $i
	else 	
		echo  " -- ERROR" $i
	fi
done
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

