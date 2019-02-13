import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.ToolRunner;
import java.io.IOException;

public class FlightCancellation extends Configured implements org.apache.hadoop.util.Tool {
    public int run(String[] args) throws Exception{
        try{
            FileSystem fs = FileSystem.get(getConf());
            Job flightCancellationJob = Job.getInstance(getConf());
            flightCancellationJob.setJarByClass(getClass());
            org.apache.hadoop.mapreduce.lib.input.FileInputFormat.setInputPaths(flightCancellationJob, new Path(args[0]));

            flightCancellationJob.setMapperClass(CancellationMapper.class);
            flightCancellationJob.setMapOutputKeyClass(Text.class);
            flightCancellationJob.setMapOutputValueClass(LongWritable.class);

            flightCancellationJob.setReducerClass(CancellationReducer.class);
            flightCancellationJob.setOutputKeyClass(Text.class);
            flightCancellationJob.setOutputValueClass(LongWritable.class);

            org.apache.hadoop.mapreduce.lib.output.FileOutputFormat.setOutputPath(flightCancellationJob, new Path(args[1]));

            if (fs.exists(new Path(args[1])))
                fs.delete(new Path(args[1]), true);

            flightCancellationJob.waitForCompletion(true);
            return flightCancellationJob.waitForCompletion(true) ? 0 : 1;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return 0;
    }
    public static void main(String[] args) throws Exception{
        System.exit(ToolRunner.run(new FlightCancellation(), args));
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
class CancellationMapper extends Mapper<LongWritable, Text, Text, LongWritable>
{
    public void map(LongWritable lineOffSet, Text record, Context context) throws IOException, InterruptedException
    {
        String[] tokens = record.toString().split(",");

        String cancel = tokens[21];
        String code = tokens[22];
        if(!cancel.equals("NA") && !cancel.equals("Cancelled") && cancel.equals("1") &&
                !code.equals("NA") && !code.equals("CancellationCode") && !code.isEmpty()){
            context.write(new Text(code), new LongWritable(1));
        }
        /*if(tokens[0] != null && !tokens[22].equals("CancellationCode") && !tokens[22].equals("NA"))
        {
            String cancellationCode = tokens[22];

            if (cancellationCode != null && !cancellationCode.trim().equals(""))
                context.write(new Text(cancellationCode), new LongWritable(1));
        }*/
    }
}

class CancellationReducer<KEY> extends Reducer<Text, LongWritable, Text, LongWritable>
{
    long max = 0L;
    Text maxCancellationCode = new Text();

    protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
        long sum = 0;

        for (LongWritable val : values)
            sum += val.get();

        if (sum >= max) {
            maxCancellationCode.set(key);
            max = sum;
        }
    }
        protected void cleanup(Context context) throws IOException, InterruptedException
        {
            if(maxCancellationCode.toString().equals("A")) {
                context.write(new Text("Most Common Reason for Flight Cancellation is: Carrier"), null);
            }
            else if(maxCancellationCode.toString().equals("B")){
                context.write(new Text("Most Common Reason for Flight Cancellation is: Weather"), null);
            }
            else if(maxCancellationCode.toString().equals("C")){
                context.write(new Text("Most Common Reason for Flight Cancellation is: NAS"), null);
            }
            else if(maxCancellationCode.toString().equals("D")){
                context.write(new Text("Most Common Reason for Flight Cancellation is: Security "), null);
            }
            else
                context.write(new Text("There are no cancellations according to the input files."), null);
        }

}