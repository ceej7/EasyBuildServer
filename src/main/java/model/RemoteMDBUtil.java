package model;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RemoteMDBUtil {
    //remote
    public static ServerAddress seed1 = new ServerAddress("dds-bp1375253460cc84-pub.mongodb.rds.aliyuncs.com", 3717);
    public static String username = "root";
    public static String password = "Aa123456";
    public static String DEFAULT_DB = "admin";
    public static String DEMO_DB = "building";
    public static String DEMO_COLL = "items";
    public static MongoClient createMongoDBClient() {
        // 构建Seed列表
        List<ServerAddress> seedList = new ArrayList<ServerAddress>();
        seedList.add(seed1);
        // 构建鉴权信息
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(MongoCredential.createScramSha1Credential(username,
                DEFAULT_DB, password.toCharArray()));
        return new MongoClient(seedList, credentials);
    }
    public static void main(String args[]) {
        MongoClient client = createMongoDBClient();
        try {
            // 取得Collecton句柄
            MongoDatabase database = client.getDatabase(DEMO_DB);
            MongoCollection<Document> collection = database.getCollection(DEMO_COLL);
            // 插入数据
            Document doc = new Document();
            // 读取数据
            MongoCursor<Document> cursor = collection.find().iterator();
            int i=0;
            while (cursor.hasNext()) {
                System.out.println(i+"   find document: " + cursor.next());
                i++;
            }
        } finally {
            //关闭Client，释放资源
            client.close();
        }
        return ;
    }
}
