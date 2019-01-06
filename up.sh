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
echo "Conccurency:   = ${CONCURRENCY}"
echo "Time:          = ${TIME}"
echo "Load Raw Data: = ${LOADRAW}"
echo "Initialize DB: = ${DB}"
echo "Initialize KS: = ${KS}"
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
echo ">>> updating java application from git repository:"
cd ~/Jepsen_Java_Tests/ 
git pull
cd - >/dev/null

if [ ${LOADRAW} = "true" ]; then
	# copying necessary files to the root dir
	cp /home/ubuntu/Jepsen_Java_Tests/table.names /home/ubuntu/table.names
	# copying necessary files to jepsen nodes
	while IFS='' read -r line || [[ -n "$line" ]]; do
	    echo ">>> copying resource files to node: $line"
	    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/table.names ubuntu@${line}:/home/ubuntu/resource/ >/dev/null
	    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/ddl.cql ubuntu@${line}:/home/ubuntu/resource/ >/dev/null
	    while IFS='' read -r table || [[ -n "$table" ]]; do
	    	scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/load_${table}.cql ubuntu@${line}:/home/ubuntu/resource/ >/dev/null
	    done < "/home/ubuntu/table.names" 
	    echo "done."
    	done < "/home/ubuntu/nodes"
fi

#scp -r -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/snapshots/seats ubuntu@n1:/home/ubuntu/

echo ""
echo ">>> calling Jepsen:"
time lein run test --nodes-file ~/nodes  --concurrency ${CONCURRENCY} --time-limit ${TIME} ${DB} ${KS}  --username ubuntu --ssh-private-key ~/.ssh/ec2-ohio.pem









