cd ~/Jepsen_Java_Tests/ 
git pull
cd -
# copying necessary files to jepsen nodes
while IFS='' read -r line || [[ -n "$line" ]]; do
    echo "Copying Necessary Files to $line:"
    cp /home/ubuntu/Jepsen_Java_Tests/table.names /home/ubuntu/table.names
    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/table.names ubuntu@${line}:/home/ubuntu/resource/
    scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/ddl.cql ubuntu@${line}:/home/ubuntu/resource/
    while IFS='' read -r table || [[ -n "$table" ]]; do
    	scp -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/Jepsen_Java_Tests/load_${table}.cql ubuntu@${line}:/home/ubuntu/resource/
    done < "/home/ubuntu/table.names" 
done < "/home/ubuntu/nodes"

#scp -r -i "/home/ubuntu/.ssh/ec2-ohio.pem" /home/ubuntu/snapshots/seats ubuntu@n1:/home/ubuntu/

time lein run test --nodes-file ~/nodes  --concurrency $1 --time-limit 25 --init-db --init-ks  --username ubuntu --ssh-private-key ~/.ssh/ec2-ohio.pem
