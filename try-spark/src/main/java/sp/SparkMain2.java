package sp;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
/**
 * Created by denny on 4/21/16.
 */
public class SparkMain2 {
    public static void main(String[] args) {
        SparkConf conf = new SparkConf().setMaster("local").setAppName("My App2");
        JavaSparkContext sc = new JavaSparkContext(conf);

        JavaRDD<String> rrd1 = sc.textFile("/home/denny/tmp/1.php");

        System.out.println("count="+rrd1.count()+ " First="+rrd1.first());

    }
}
