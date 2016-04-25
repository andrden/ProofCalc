package sp;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

/**
 * Created by denny on 4/21/16.
 */
public class SparkMain2 {

    // run on cluster as114 as

    // gradle jar
    // spark-submit --class sp.SparkMain2 build/libs/try-spark.jar

    public static void main(String[] args) {
        // local, yarn-client
        SparkConf conf = new SparkConf().setMaster("yarn-client").setAppName("My App21");
        JavaSparkContext sc = new JavaSparkContext(conf);
        SQLContext sql = new org.apache.spark.sql.SQLContext(sc);

        JavaRDD<String> rrd1 = sc.textFile("/tmp/15lines.txt");
        System.out.println("count="+rrd1.count()+ " First="+rrd1.first());

        DataFrame df = sql.read().parquet("/adslogs/epommarket.com/day=2016-04-25/ev=impressions/srv=6/2016-04-25-00.parquet.gz");
        df.registerTempTable("parquetFile");
        DataFrame count1 = sql.sql("SELECT count(*) FROM parquetFile ");
        System.out.println("count1="+count1);
        count1.show();


    }
}
