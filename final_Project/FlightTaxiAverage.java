import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

public class FlightTaxiAverage extends Configured implements Tool{
    @Override
    public int run(String[] strings) throws Exception {
        try{
            FileSystem fs = FileSystem.get(getConf());
            Job TaxiAvgJob =  Job.getInstance(getConf());
            TaxiAvgJob.setJarByClass(getClass());
            FileInputFormat.setInputPaths(TaxiAvgJob, new Path(strings[0]));

            TaxiAvgJob.setMapperClass(TaxiAverageMapper.class);
            TaxiAvgJob.setMapOutputKeyClass(Text.class);
            TaxiAvgJob.setMapOutputValueClass(Text.class);

            TaxiAvgJob.setReducerClass(TaxiAverageReducer.class);
            TaxiAvgJob.setMapOutputKeyClass(Text.class);
            TaxiAvgJob.setMapOutputValueClass(LongWritable.class);

            FileOutputFormat.setOutputPath(TaxiAvgJob, new Path(strings[1]));


            TaxiAvgJob.waitForCompletion(true);
            return TaxiAvgJob.waitForCompletion(true) ? 0: 1;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return 0;
    }
    public static void main(String[] arg) throws Exception{
        System.exit(ToolRunner.run(new FlightTaxiAverage(), arg));
    }
}

class TaxiAverageMapper extends Mapper<Object, Text, Text, LongWritable>{
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException
    {
        String[] tokens = value.toString().split(",");
        String dest = tokens[17];
        String taxiIn = tokens[19];
        String origin = tokens[16];
        String taxiOut = tokens[20];

        if(!dest.equals("NA") && !dest.equals("Dest") && !taxiIn.equals("NA"))
            context.write(new Text(dest), new LongWritable(Integer.parseInt(taxiIn)));

        if(!origin.equals("NA") && !origin.equals("Origin") && !taxiOut.equals("NA"))
            context.write(new Text(origin), new LongWritable(Integer.parseInt(taxiOut)));
    }
}

class TaxiAverageReducer extends Reducer<Text, LongWritable, Text, DoubleWritable>{
    double max1 = 0, max2 = 0, max3 = 0;
    double min1 = Double.MAX_VALUE, min2 = Double.MAX_VALUE, min3 = Double.MAX_VALUE;
    Text taxiMax1 = new Text();
    Text taxiMax2 = new Text();
    Text taxiMax3 = new Text();
    Text taxiMin1 = new Text();
    Text taxiMin2 = new Text();
    Text taxiMin3 = new Text();

    public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException
    {
        int count = 0, taxiSum = 0;

        for (LongWritable val : values)
        {
            count += 1;
            taxiSum += val.get();
        }

        double avg = (double)taxiSum / (double)count;

        if (avg > max1)
            exchangeMax1(avg, key.toString());
        else if (avg > max2)
            exchangeMax2(avg, key.toString());
        else if (avg > max3)
            exchangeMax3(avg, key.toString());

        if (avg < min1)
            exchangeMin1(avg, key.toString());
        else if (avg < min2)
            exchangeMin2(avg, key.toString());
        else if (avg < min3)
            exchangeMin3(avg, key.toString());

    }

    public void exchangeMax1(double avg, String key)
    {
        max3 = max2;
        taxiMax3.set(taxiMax2.toString());
        max2 = max1;
        taxiMax2.set(taxiMax1.toString());
        max1 = avg;
        taxiMax1.set(key.toString());
    }

    public void exchangeMax2(double avg, String key)
    {
        max3 = max2;
        taxiMax3.set(taxiMax2.toString());
        max2 = avg;
        taxiMax2.set(key.toString());
    }

    public void exchangeMax3(double avg, String key)
    {
        max3 = avg;
        taxiMax3.set(key.toString());
    }

    public void exchangeMin1(double avg, String key)
    {
        min3 = min2;
        taxiMin3.set(taxiMin2.toString());
        min2 = min1;
        taxiMin2.set(taxiMin1.toString());
        min1 = avg;
        taxiMin1.set(key.toString());
    }

    public void exchangeMin2(double avg, String key)
    {
        min3 = min2;
        taxiMin3.set(taxiMin2.toString());
        min2 = avg;
        taxiMin2.set(key.toString());
    }

    public void exchangeMin3(double avg, String key)
    {
        min3 = avg;
        taxiMin3.set(key.toString());
    }

    protected void cleanup(Context context) throws IOException, InterruptedException
    {
        if(max1==0.0 && max2==0.0 && max3==0.0)
            context.write(new Text("There are no Taxi times for given input(s)"), null);
        else{
            context.write(new Text("The following are the 3 longest average taxi time "), null);
            context.write(taxiMax1,new DoubleWritable(max1));
            if(max2==0.0)
                context.write(new Text("There are no further taxi times"), null);
            else
                context.write(taxiMax2, new DoubleWritable(max2));

            if(min3 == 0.0)
                context.write(new Text("There are no further taxi times."), null);
            else
                context.write(taxiMax3, new DoubleWritable(max3));

        }

        context.write(null, null);

        if(min1==Double.MAX_VALUE && min2==Double.MAX_VALUE && min3==Double.MAX_VALUE)
            context.write(new Text("There are no Taxi times for given input(s)."), null);
        else {
            context.write(new Text("The following are the 3 shortest average taxi time."), null);
            context.write(taxiMin1,new DoubleWritable(min1));
            if(min2==0.0)
                context.write(new Text("There are no further taxi times."), null);
            else
                context.write(taxiMin2, new DoubleWritable(min2));

            if(min3==0.0)
                context.write(new Text("There are no further taxi times."), null);
            else
                context.write(taxiMin3, new DoubleWritable(min3));
        }
    }
}