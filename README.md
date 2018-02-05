# Torrent


GPU and distributed architecture computing, Babeș - Bolyai University, Cluj - Napoca

1. The communication is done using the Google Protobuffer 3.x messages defined below, over TCP. The exchange will be
   synchronous. When sending a request, open the TCP connection, send the message, get back the response, then close
   the connection. When listening for requests, send back the response, then close the socket.

2. The system consists of several nodes and one client. Your job is to implement the node. The client will be provided
   to you by the instructor.

3. The evaluation will be done as follows:
       - Your laptops will connect to a small wired switch and have predefined static addresses
       - The instructor's laptop will also be connected to the switch and test your nodes using the client
       - You will run 3 nodes on your laptop, each on a predefined port
       - Your nodes will communicate with your colleagues and the instructor's nodes
       - Initially your nodes will have no data. The instructor's client will upload various files to various nodes,
         and ask you to replicate locally some of those files. It will also download files form you to make sure they
         are correct.

4. Node referencing: as all IP addresses are known ahead of time and belong to the same network, and the ports are
   predefined, the nodes will be referred, for convenience, as <ip-suffix>:<port-offset>. This is not necessarily
   relevant for your implementation, but it is useful understanding it. See below for beter understanding

5. You should have a configuration of some sort that contains the folowing information
       - ip-prefix 127.0.0
       - port-base 5000
       - ip-suffixes 2 3 4 5 6 7 8 9
       - port-offsets 1 2 3
   Using this configuration, you will easly know all the nodes in the network. All this will be given to you during the
   evaluation, and you should be able to easily apply the configuration to you implementation. Following the convention
   at point 4, a node reference of 1:3, refers to a node running on host 127.0.0.1 and port 5003. You do not need to use
   this convention, but the instructor will identify your nodes in this manner. Be prepared to handle nodes that are
   offline.

6. Implement handlers for al the messages below that are supposed to be handled by the node. See the "Client -> Node"
   and "Node -> Node" markers of each message. For instance, you should handle a ReplicateRequest, and send back a
   ReplicateResponse. You should also be able to handle a ChunkRequest, but also send a ChunkRequest.

7. Every response message contains a status field and an error message.

8. The files/chunks that you receive, do not have to be stored on disk, they can be kept in memory just as well.

9. The standard chunk size will be 1024, with the last chunk being usually smaller.

10. The first chunk index is 0.

11. When broadcasting a request to all nodes, aggregate in the response, everything that you get from other nodes,
    including failed responses, and all responses from the same node. Basically, keep everything. The client will
    rely on these for evaluating your implementation. For example, when you get a ReplicateRequest, you need to send
    ChunkRequests to all nodes. Some will give you ChunkReponses with data, others with error. Sometimes, you might have
    to ask the same node multiple times. All these reponses must be stored in the ReplicateResponse.

12. In order to determine the type of the message you receive, all messages should be wrapped in the wrapper message.
        - Sending example in a mix of languages
            LocalSearchRequest lsr = ...
            Message msg = Message.newBuilder()
                                 .setType(Message.Type.LOCAL_SEARCH_REQUEST)
                                 .setLocalSearchRequest(lsr)
                                 .build();
            byte[] m = msg.toByteArray();
            int len = m.length; // 32-bit integer
            len = convert-big-endian(len);
            send(len)
            send(m)
        - Receiving example in a mix of languages
            int len = receive(sizeof(int)) // 32-bit integer
            len = convert-to-local-endianness(len);
            byte[] m = receive(len);
            Message msg = Message.parse(m);
            if(msg.hasLocalSearchRequest()) {
                do-stuff(msg.getLocalSearchRequest());
            }
