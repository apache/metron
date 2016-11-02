var parse =function(rawMessage){
	parts=rawMessage.split("|");
	var message = new MetronMessage("userlog");
	message["customerId"]=parts[1];
	message["first_name"]=parts[2];
	message["last_name"]=parts[3];
	message["age"]=parts[4];
	message["login-time"]=parts[0];
	message["ip-address"]=parts[5];
	message["os"]=parts[6];
	message["device"]=parts[7];
	return message;
};