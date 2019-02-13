package mapReduce.mapReduce;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class missingCards {

	public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		Configuration config = new Configuration();
        Job job = new Job(config, "Finding Missing Cards");
        
        job.setJarByClass(missingCards.class);
        
        job.setMapperClass(map.class);
        job.setReducerClass(reduce.class);
        
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        boolean status = job.waitForCompletion(true);
        if(status) {
        	System.exit(0);
        }
        else {
        	System.exit(1);
        }

	}
	//extend mapper class
	public static class map extends Mapper<LongWritable, Text, Text, IntWritable>{
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
			String line = value.toString();
	        String[] lineSplit = line.split(",");
	        Text textNew = new Text(lineSplit[0]);
	        IntWritable intWritable = new IntWritable(Integer.parseInt(lineSplit[1]));
	        context.write(textNew, intWritable);
		}
	}
	//extend reducer class
	public static class reduce extends Reducer<Text, IntWritable, Text, IntWritable>{
		public void reduce(Text key, Iterable<IntWritable> value, Context context) throws IOException, InterruptedException {
			
			ArrayList<Integer> fullDeck = new ArrayList<Integer>();
			
			for(int i = 0; i < 14; i++) {
				fullDeck.add(i);
			}
			
			int cardCount = 0, cardVal = 0;
			int size = 0;
			
			for(IntWritable cards: value) {
				cardCount++;
				cardVal = cards.get();
				if(fullDeck.contains(cardVal)) {
					int index = fullDeck.indexOf(cardVal);
					fullDeck.remove(index);
				}
			}
			
			cardCount = 13 - cardCount;
			if(cardCount <= 13 && cardCount >=0) {
				size = fullDeck.size();
				int counter = 0;
				while(counter < size) {
					IntWritable intWritable = new IntWritable(fullDeck.get(counter));
					context.write(key, intWritable);
					counter++;
				}
			}
		}
	}

}
