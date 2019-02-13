import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FlightScheduleProb extends Configured implements org.apache.hadoop.util.Tool {
    public int run(String[] args) throws Exception{
        try{
            FileSystem fs = FileSystem.get(getConf());
            Job flightScheduleJob = Job.getInstance(getConf());
            flightScheduleJob.setJarByClass(getClass());
            org.apache.hadoop.mapreduce.lib.input.FileInputFormat.setInputPaths(flightScheduleJob, new Path(args[0]));

            flightScheduleJob.setMapperClass(OnScheduleMapper.class);
            flightScheduleJob.setMapOutputKeyClass(Text.class);
            flightScheduleJob.setMapOutputValueClass(LongWritable.class);

            flightScheduleJob.setReducerClass(OnScheduleReducer.class);
            flightScheduleJob.setOutputKeyClass(Text.class);
            flightScheduleJob.setOutputValueClass(LongWritable.class);

            org.apache.hadoop.mapreduce.lib.output.FileOutputFormat.setOutputPath(flightScheduleJob, new Path(args[1]));

            if (fs.exists(new Path(args[1])))
                fs.delete(new Path(args[1]), true);

            flightScheduleJob.waitForCompletion(true);
            return flightScheduleJob.waitForCompletion(true) ? 0 : 1;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return 0;
    }
    public static void main(String[] args) throws Exception{
        System.exit(ToolRunner.run(new FlightScheduleProb(), args));
    }
    public static class StringComparator extends WritableComparator
    {

        public StringComparator()
        {
            super(Text.class);
        }

        @Override
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2)
        {

            try
            {
                String v1 = Text.decode(b1, s1, l1);
                String v2 = Text.decode(b2, s2, l2);

                return v1.compareTo(v2);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException(e);
            }
        }
    }
}

class OnScheduleMapper extends Mapper<LongWritable, Text, Text, LongWritable>
{
    public void map(LongWritable lineOffSet, Text record, Context context) throws IOException, InterruptedException
    {
        String[] strArr = record.toString().split(",");

        if (strArr[0] != null && !strArr[8].equals("UniqueCarrier"))
        {
            String uniqueCarrier = strArr[8];
            String arrDelay = strArr[14];

            if (uniqueCarrier != null && arrDelay != null && !uniqueCarrier.trim().equals("") && !arrDelay.trim().equals(""))
            {
                try
                {
                    Integer arrDelayInt = Integer.parseInt(arrDelay);
                    context.write(new Text("All: " + uniqueCarrier), new LongWritable(1));

                    if (arrDelayInt > 10)
                        context.write(new Text("Delayed: " + uniqueCarrier), new LongWritable(1));
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}
 class OnScheduleReducer<KEY> extends Reducer<Text, LongWritable, Text, DoubleWritable>
{
    Map<String, Long> hm = new HashMap<String, Long>();

    double max1 = 0L, max2 = 0L, max3 = 0L;
    double min1 = Double.MAX_VALUE, min2 = Double.MAX_VALUE, min3 = Double.MAX_VALUE;
    Text uniqueCarrierMax1 = new Text();
    Text uniqueCarrierMax2 = new Text();
    Text uniqueCarrierMax3 = new Text();
    Text uniqueCarrierMin1 = new Text();
    Text uniqueCarrierMin2 = new Text();
    Text uniqueCarrierMin3 = new Text();

    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
    {
        try
        {
            if(key.toString().contains("All: "))
            {
                String uniqueCarrier = key.toString().replace("All: ", "");
                long sumTotal = 0;

                for (LongWritable val : values)
                    sumTotal++;

                hm.put(uniqueCarrier, sumTotal);
            }
            else if(key.toString().contains("Delayed: "))
            {
                long sum = 0;
                String uniqueCarrier = key.toString().replace("Delayed: ", "");
                long sumTotal = hm.get(uniqueCarrier);

                for (LongWritable val : values)
                    sum++;

                double dprobability = sum / (double) sumTotal;
                double probability = 1 - dprobability;
                
                if (probability > max1)
                    exchangeMax1(probability, uniqueCarrier);
                else if (probability > max2)
                    exchangeMax2(probability, uniqueCarrier);
                else if (probability > max3)
                    exchangeMax3(probability, uniqueCarrier);

                if (probability < min1)
                    exchangeMin1(probability, uniqueCarrier);
                else if (probability < min2)
                    exchangeMin2(probability, uniqueCarrier);
                else if (probability < min3)
                    exchangeMin3(probability, uniqueCarrier);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void exchangeMax1(double probability, String uniqueCarrier)
    {
        max3 = max2;
        uniqueCarrierMax3.set(uniqueCarrierMax2.toString());
        max2 = max1;
        uniqueCarrierMax2.set(uniqueCarrierMax1.toString());
        max1 = probability;
        uniqueCarrierMax1.set(uniqueCarrier);
    }

    public void exchangeMax2(double probability, String uniqueCarrier)
    {
        max3 = max2;
        uniqueCarrierMax3.set(uniqueCarrierMax2.toString());
        max2 = probability;
        uniqueCarrierMax2.set(uniqueCarrier);
    }

    public void exchangeMax3(double probability, String uniqueCarrier)
    {
        max3 = probability;
        uniqueCarrierMax3.set(uniqueCarrier);
    }

    public void exchangeMin1(double probability, String uniqueCarrier)
    {
        min3 = min2;
        uniqueCarrierMin3.set(uniqueCarrierMin2.toString());
        min2 = min1;
        uniqueCarrierMin2.set(uniqueCarrierMin1.toString());
        min1 = probability;
        uniqueCarrierMin1.set(uniqueCarrier);
    }

    public void exchangeMin2(double probability, String uniqueCarrier)
    {
        min3 = min2;
        uniqueCarrierMin3.set(uniqueCarrierMin2.toString());
        min2 = probability;
        uniqueCarrierMin2.set(uniqueCarrier);
    }

    public void exchangeMin3(double probability, String uniqueCarrier)
    {
        min3 = probability;
        uniqueCarrierMin3.set(uniqueCarrier);
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException
    {
        context.write(new Text("Flights with Highest probability for being on schedule"), null);
        context.write(uniqueCarrierMax1, new DoubleWritable(max1));
        context.write(uniqueCarrierMax2, new DoubleWritable(max2));
        context.write(uniqueCarrierMax3, new DoubleWritable(max3));
        context.write(null, null);
        context.write(new Text("Flights with Lowest probability for being on schedule"), null);
        context.write(uniqueCarrierMin1, new DoubleWritable(min1));
        context.write(uniqueCarrierMin2, new DoubleWritable(min2));
        context.write(uniqueCarrierMin3, new DoubleWritable(min3));
    }
}