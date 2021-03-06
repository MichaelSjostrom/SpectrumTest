package com.somitsolutions.android.spectrumanalyzer;


import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;
import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import java.util.Random;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.uol.aig.fftpack.RealDoubleFFT;
import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;


public class SoundRecordAndAnalysisActivity extends Activity implements OnClickListener{

	
	int sampleRate = 16000; //16000;//8000;//44100;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    float x = (float) (2 * Math.PI);
    
    short[] buffer2 = new short[15*4*2048];

    
    double maxFreq=0.0;
    double maxAmp2[] = new double[2];

    AudioRecord audioRecord;
    private RealDoubleFFT transformer;
    int blockSize;// = 256;
    Button startStopButton;
    ResetButton resetBaseFreqBtn;
    boolean started = false;

    RecordAudio recordTask;
    ImageView imageViewDisplaySectrum;
    MyImageView imageViewScale;
    Bitmap bitmapDisplaySpectrum;
   
    Canvas canvasDisplaySpectrum;
    
    
    Paint paintSpectrumDisplay;
    Paint paintScaleDisplay;
    //making the "paintSpectrumDisplay" fade
    Paint fadePaint;
    
    static SoundRecordAndAnalysisActivity mainActivity;
    LinearLayout main;
    int width;
    int height;
    int left_Of_BimapScale;
    int left_Of_DisplaySpectrum;
    private final static int ID_BITMAPDISPLAYSPECTRUM = 1;
    private final static int ID_IMAGEVIEWSCALE = 2;
    
    FreqView freqText = null;
    double[] newSpectra;
    
	//F�R ATT GENERERA TONER MED VISSA FREKVENSER SAMT LITE LAYOUT - RICKARD
    FreqView generatedTone;
    
    int minFreqGen = 500;
    int maxFreqGen = 2500;
    int nrOfPoints = 0;
    
	Button genToneButton;
	Button genWheelTone;
	private final int duration = 4;
	private final int numSamples = duration * 44100;
	
	private final double sample[] = new double[15*8*1024];
    private double freqOfTone; //HZ
	
    private final byte generatedSnd[] = new byte[2 * numSamples];
    
    String wheelMenuFreq[];
    
    NumericWheelAdapter freqAdapter;
    
    private WheelView freqWheel;
    private final int freqWheelId = 1337;
    
    private boolean wheelScrolled = false;
    
    private TextView textView;
    private EditText editText;
    
    double step;
    
    
    
    boolean getPoints = false;
    

	//
    
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
    	
    	width = display.getWidth();
    	height = display.getHeight();
    	
    	Log.i("freq", "booog2323");
    	
    	
    	blockSize = 480;//256;//32768/2;//256;//32768;
    	newSpectra = new double[blockSize];
    		//}  
    	freqText = new FreqView(this);
    	freqText.setTextColor(0xffff7700);
    		
    	generatedTone = new FreqView(this);
    	generatedTone.setTextColor(0xffff7700);
    }

 
    
    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
    	//left_Of_BimapScale = main.getC.getLeft();
    	MyImageView  scale = (MyImageView)main.findViewById(ID_IMAGEVIEWSCALE);
    	ImageView bitmap = (ImageView)main.findViewById(ID_BITMAPDISPLAYSPECTRUM);
    	left_Of_BimapScale = scale.getLeft();
    	left_Of_DisplaySpectrum = bitmap.getLeft();
    }
    private class RecordAudio extends AsyncTask<Void, double[], Void> {
    	
        @Override
        protected Void doInBackground(Void... params) {
        	
        	if(isCancelled()){
        		return null;
        	}
        //try {
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    channelConfiguration, audioEncoding);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, sampleRate,
                    	channelConfiguration, audioEncoding, bufferSize);
                    
            int bufferReadResult;
            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize]; 
           // double[] toTransform2 = new double[blockSize];
            
            try{
            	audioRecord.startRecording();
            }
            catch(IllegalStateException e){
            	Log.e("Recording failed", e.toString());
            	
            }
            while (started) {
            	
           
            		bufferReadResult = audioRecord.read(buffer, 0, blockSize);
            	//}
            	if(isCancelled())
                    	break;

            for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                
            	toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit                
            }
            

            transformer.ft(toTransform);
            
            publishProgress(toTransform);
            
            if(isCancelled())
            	break;
            	//return null;
            }
            
            try{
            	audioRecord.stop();
            }
            catch(IllegalStateException e){
            	Log.e("Stop failed", e.toString());
            	
            }               
            
            return null;
        }
        
        protected void onProgressUpdate(double[]... toTransform) {

        	if (width >= 480) {
        		
        		
        		//double maxAmp =0;
        		int upy = 300;
        		maxAmp2 = new double[2];
        		for (int i = 0; i < toTransform[0].length; i++) {
                    int downy = (int) (upy - Math.abs(toTransform[0][i] * 5));
                    
                    if(Math.abs(toTransform[0][i]) > maxAmp2[0]) {
                    	maxAmp2[0] = Math.abs(toTransform[0][i]);
                    	maxAmp2[1] = i;
                    }
                    canvasDisplaySpectrum.drawLine(i, downy, i, upy, paintSpectrumDisplay);
                    
                    
                }
        		//Log.i("maxAmp","maxAmp in freq. spec. : " + maxAmp+"\nNo. of frequency slots: " + toTransform[0].length);
        		
        		calcBaseFrequency(toTransform);
        		
        		//Fade last amplitudes
        		canvasDisplaySpectrum.drawPaint(fadePaint);
        		
        		imageViewDisplaySectrum.invalidate();
        	}
        	else if (width > 512){
        		
        		for (int i = 0; i < toTransform[0].length; i++) {
                    int x = 2*i;
                    int downy = (int) (150 - (toTransform[0][i] * 10));
                    int upy = 150;
                    canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
                    }
                    
                    imageViewDisplaySectrum.invalidate();
                    
            }
        	
        	else{
        		
        		for (int i = 0; i < toTransform[0].length; i++) {
        			int x = i;
                    int downy = (int) (150 - (toTransform[0][i] * 10));
                    int upy = 150;
                    canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
                    }
                    
                imageViewDisplaySectrum.invalidate();
        	}
        	
        	//Log.i("log", "" + (int)(maxAmp2[1]*step))
                
        }
        
        protected void onPostExecute(Void result) {
        	try{
            	audioRecord.stop();
            }
            catch(IllegalStateException e){
            	Log.e("Stop failed", e.toString());
            	
            }
        	recordTask.cancel(true); 
        	//}
        	Intent intent = new Intent(Intent.ACTION_MAIN);
        	intent.addCategory(Intent.CATEGORY_HOME);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);
        }
                
       }
            
        

   
    public void onClick(View v) {
    	if(v == startStopButton) {
	        if (started == true) {
		        started = false;
		        startStopButton.setText("Start");
		        recordTask.cancel(true);
		        audioRecord.release();

	        } 
	        else {
	        	Log.i("log", "started=false");
	        	canvasDisplaySpectrum.drawColor(Color.BLACK);
		        started = true;
		        startStopButton.setText("Stop");
		        recordTask = new RecordAudio();
		        recordTask.execute();
		        newSpectra = new double[blockSize];
                maxAmp2 = new double[2];
                freqText.setText("Base frequency = 0 Hz\nTop amplitude at: 0 Hz\nPoints: ");
	        }  
    	}
    	else if(v == genToneButton){
    		
    		getPoints = true;
    		genFreq();
    		genTone();

    		//playSound();
    		

    		//playSound();
    		freqAdapter.setItemResource((int) freqOfTone);
    		freqWheel.setCurrentItem(freqAdapter.getItemResource());

    		generatedTone.setText("Generated tone: " + freqOfTone + " Hz");
    	}
    	else if(v == genWheelTone){
    		freqOfTone = freqWheel.getCurrentItem();
    		genTone();
    		//playSound();
    		generatedTone.setText("Generated tone: " + freqOfTone + " Hz");

    	}
     }
    
        static SoundRecordAndAnalysisActivity getMainActivity(){
        	return mainActivity;
        }
        
        public void onStop(){
        	super.onStop();
        	
        	recordTask.cancel(true); 

            Intent intent = new Intent(Intent.ACTION_MAIN);
        	intent.addCategory(Intent.CATEGORY_HOME);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);
        }
        
        public void onStart(){
        	
        	super.onStart();
        	main = new LinearLayout(this);
        	main.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));
        	main.setOrientation(LinearLayout.VERTICAL);
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
        	newSpectra = new double[blockSize];
        	main.addView(freqText,
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        	
        	
        	transformer = new RealDoubleFFT(blockSize);
            
            imageViewDisplaySectrum = new ImageView(this);
            if(width >= 480){
            	bitmapDisplaySpectrum = Bitmap.createBitmap((int)480,(int)300,Bitmap.Config.ARGB_8888);
            }
            else if(width > 512){
            	bitmapDisplaySpectrum = Bitmap.createBitmap((int)512,(int)300,Bitmap.Config.ARGB_8888);
            } 
            else{
            	 bitmapDisplaySpectrum = Bitmap.createBitmap((int)256,(int)150,Bitmap.Config.ARGB_8888);
            }
            LinearLayout.LayoutParams layoutParams_imageViewScale = null;
            //Bitmap scaled = Bitmap.createScaledBitmap(bitmapDisplaySpectrum, 320, 480, true);
            canvasDisplaySpectrum = new Canvas(bitmapDisplaySpectrum);
            //canvasDisplaySpectrum = new Canvas(scaled);
            paintSpectrumDisplay = new Paint();
            fadePaint = new Paint();
            
            paintSpectrumDisplay.setColor(Color.YELLOW);
            fadePaint.setColor(Color.argb(220, 255, 255, 255));//Adjust alpha(first position) to change how quickly the image fades
            fadePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
            
            imageViewDisplaySectrum.setImageBitmap(bitmapDisplaySpectrum);
           
            if ((width >= 480)){
            	LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(0, 20, 0, 0);
               //layoutParams_imageViewDisplaySpectrum.gravity = Gravity.CENTER_HORIZONTAL;
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
                
            	//imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            	layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            	((MarginLayoutParams) layoutParams_imageViewScale).setMargins(0, 20, 0, 100);
            	//layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
            }
            else if(width >512){
            	//imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            	LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(100, 600, 0, 0);
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
                layoutParams_imageViewScale= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                //layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
                ((MarginLayoutParams) layoutParams_imageViewScale).setMargins(100, 20, 0, 0);
                
            }
                       
            else if ((width >320) && (width<512)){
            	LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(60, 250, 0, 0);
               //layoutParams_imageViewDisplaySpectrum.gravity = Gravity.CENTER_HORIZONTAL;
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
                
            	//imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            	layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            	((MarginLayoutParams) layoutParams_imageViewScale).setMargins(60, 20, 0, 100);
            	//layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
            }
           
            else if (width < 320){
            	/*LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(30, 100, 0, 100);
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);*/
            	imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            	layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            	//layoutParams_imageViewScale.gravity = Gravity.CENTER;
            }
            imageViewDisplaySectrum.setId(ID_BITMAPDISPLAYSPECTRUM);
            main.addView(imageViewDisplaySectrum);
            
            
            //((MarginLayoutParams) layoutParams_imageViewScale).setMargins(0, 20, 0, 20);
            
            imageViewScale = new MyImageView(this);
            imageViewScale.setLayoutParams(layoutParams_imageViewScale);
            imageViewScale.setId(ID_IMAGEVIEWSCALE);
            
            //imageViewScale.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            main.addView(imageViewScale);
            
            
            startStopButton = new Button(this);
            startStopButton.setText("Start");
            startStopButton.setOnClickListener(this);
            startStopButton.setLayoutParams(new LinearLayout.LayoutParams(width/2,LinearLayout.LayoutParams.WRAP_CONTENT));
           
            resetBaseFreqBtn = new ResetButton(this);
            //resetBaseFreqBtn.setText("Reset Base frequency");
            //resetBaseFreqBtn.setOnClickListener(this);
            resetBaseFreqBtn.setLayoutParams(new LinearLayout.LayoutParams(width/2,LinearLayout.LayoutParams.WRAP_CONTENT));
            resetBaseFreqBtn.setVerticalFadingEdgeEnabled(true);
            //resetBaseFreqBtn.setBackgroundColor(Color.RED);
            
            LinearLayout ll = new LinearLayout(this);
        	ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        	ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.addView(startStopButton);

            ll.addView(resetBaseFreqBtn);
            main.addView(ll);
            
            genToneButton = new Button(this);
            genToneButton.setText("Play Random Freq");
            genToneButton.setOnClickListener(this);
            genToneButton.setLayoutParams(new LinearLayout.LayoutParams(width/2,LinearLayout.LayoutParams.WRAP_CONTENT));
            
            LinearLayout ll2 = new LinearLayout(this);
            ll2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            ll2.setOrientation(LinearLayout.HORIZONTAL);
            ll2.addView(genToneButton);
                   
            main.addView(ll2);
            
            freqWheel = new WheelView(this);
            freqWheel.setId(freqWheelId);
            freqWheel.setLayoutParams(new LinearLayout.LayoutParams(width/2,LinearLayout.LayoutParams.WRAP_CONTENT));
            
            freqAdapter = new NumericWheelAdapter(this, 0,2500); 
            freqWheel.setViewAdapter(freqAdapter);

            LinearLayout ll3 = new LinearLayout(this);
            ll3.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            ll3.setOrientation(LinearLayout.HORIZONTAL);
            ll3.addView(freqWheel);
            
            main.addView(ll3);
            
            genWheelTone = new Button(this);
            genWheelTone.setText("Generate tone");
            genWheelTone.setLayoutParams(new LinearLayout.LayoutParams(width/2,LinearLayout.LayoutParams.WRAP_CONTENT));
            genWheelTone.setOnClickListener(this);
            
            LinearLayout ll4 = new LinearLayout(this);
            ll4.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            ll4.setOrientation(LinearLayout.HORIZONTAL);
            ll4.addView(genWheelTone);

            main.addView(ll4);
            
            /*
            Dialog wheels = new Dialog(this);
            
            freqWheel = (WheelView) wheels.findViewById(R.id.w1);
            
            main.addView(freqWheel);
            
            */
            
            main.addView(generatedTone,
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
            
            setContentView(main);
            //recordTask = new RecordAudio();
            
            
            /*left_Of_BimapScale = main.getChildAt(1).getLeft();*/
            
            mainActivity = this;
            
        }
        @Override
        public void onBackPressed() {
        	super.onBackPressed();
        	//if(recordTask != null){
        		recordTask.cancel(true); 
        	//}
        	Intent intent = new Intent(Intent.ACTION_MAIN);
        	intent.addCategory(Intent.CATEGORY_HOME);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);
        }
        
        @Override
        protected void onDestroy() {
            // TODO Auto-generated method stub
            super.onDestroy();
            recordTask.cancel(true); 
            Intent intent = new Intent(Intent.ACTION_MAIN);
        	intent.addCategory(Intent.CATEGORY_HOME);
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);
        	
        	//audioRecord.release();
        	
        	
        }
        //Custom Imageview Class
        public class MyImageView extends ImageView {
        	Paint paintScaleDisplay;
        	Bitmap bitmapScale;
        	Canvas canvasScale;
        	//Bitmap scaled;
        	public MyImageView(Context context) {
        		super(context);
        		// TODO Auto-generated constructor stub
        		if(width >= 480){
        			bitmapScale =  Bitmap.createBitmap((int)480,(int)50,Bitmap.Config.ARGB_8888);
        		}
        		else if(width >512){
        			bitmapScale = Bitmap.createBitmap((int)512,(int)50,Bitmap.Config.ARGB_8888);
                }
        		else{
        			bitmapScale =  Bitmap.createBitmap((int)256,(int)50,Bitmap.Config.ARGB_8888);
        		}
        		
        		paintScaleDisplay = new Paint();
        		paintScaleDisplay.setColor(Color.WHITE);
                paintScaleDisplay.setStyle(Paint.Style.FILL);
                
                canvasScale = new Canvas(bitmapScale);
               
                setImageBitmap(bitmapScale);
                invalidate();
                
                
        	}
        	
        	@Override
            protected void onDraw(Canvas canvas)
            {
                // TODO Auto-generated method stub
                super.onDraw(canvas);
  
               
                if (width >= 480){
                  	 canvasScale.drawLine(0, 30, 0 + 480, 30, paintScaleDisplay);
                  	 int stepOf1000 = (int) 480/(sampleRate/2000);
                  	 Log.i("step of 1000", "stepOf1000 = " + stepOf1000);
                  	 int count = 0;
                  	 for(int i = 0,j = 0; i<480; i=i+stepOf1000, j++){
                       	for (int k = i; k<i+(stepOf1000); k=k+(stepOf1000/9)){
                       		if(count % 5 == 0)
                       			canvasScale.drawLine(k, 32, k, 23, paintScaleDisplay);
                       		else
                       			canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                       		count++;
                       	}
                       	canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                       	String text = Integer.toString(j) + "KHz";
                       	canvasScale.drawText(text, i, 45, paintScaleDisplay);
                       }
                  	 
                  	
                  	 
                  	 canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
                  }
                else if(width > 512){
                	 canvasScale.drawLine(0, 30,  512, 30, paintScaleDisplay);
                	for(int i = 0,j = 0; i< 512; i=i+128, j++){
                     	for (int k = i; k<(i+128); k=k+16){
                     		canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                     	}
                     	canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                     	String text = Integer.toString(j) + " KHz";
                     	canvasScale.drawText(text, i, 45, paintScaleDisplay);
                     }
                	canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
                }
                              
                else if ((width >320) && (width<512)){
                	 canvasScale.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
                	 for(int i = 0,j = 0; i<256; i=i+64, j++){
                     	for (int k = i; k<(i+64); k=k+8){
                     		canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                     	}
                     	canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                     	String text = Integer.toString(j) + " KHz";
                     	canvasScale.drawText(text, i, 45, paintScaleDisplay);
                     }
                	 canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
                }
               
                else if (width <320){
               	 canvasScale.drawLine(0, 30,  256, 30, paintScaleDisplay);
               	 for(int i = 0,j = 0; i<256; i=i+64, j++){
                    	for (int k = i; k<(i+64); k=k+8){
                    		canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                    	}
                    	canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    	String text = Integer.toString(j) + " KHz";
                    	canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    }
               	 canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
               }
                
                //canvas.drawBitmap(bitmapScale, 0, 400, paintScaleDisplay);
                //invalidate();
            }
           
        }
        
        
        
        private double calcBaseFrequency(double[]... frequencySpectra){
			
			//double[] newSpectra = new double[frequencySpectra[0].length];
			
			for(int i = 0 ; i < frequencySpectra[0].length ; i++ ) {
				newSpectra[i] += frequencySpectra[0][i]; 
				
				if(2*i < frequencySpectra[0].length) 
				{
					newSpectra[i] += frequencySpectra[0][2*i];
					if(3*i < frequencySpectra[0].length) 
					{
						newSpectra[i] += frequencySpectra[0][3*i];
						if(4*i < frequencySpectra[0].length) 
						{
							newSpectra[i] += frequencySpectra[0][4*i];
							if(5*i < frequencySpectra[0].length) 
							{
								newSpectra[i] += frequencySpectra[0][5*i];
								if(6*i < frequencySpectra[0].length) 
								{
									newSpectra[i] += frequencySpectra[0][6*i];
									if(7*i < frequencySpectra[0].length) 
									{
										newSpectra[i] += frequencySpectra[0][7*i];	
									}
								}
							}
						}
					}
				}
			}
			
			int baseFreq = 0;
			double maxAmplitude=0;
			
			for(int i = 0; i< newSpectra.length; i++) {
				if(newSpectra[i] > maxAmplitude) {
					baseFreq = i;
					maxAmplitude = newSpectra[i];
					//Log.i("damn", "Max-Amp: " + maxAmplitude + "\nFrequency: " + i);
				}
					
			}
			
			step = (double) sampleRate / (2 * blockSize);
			//Log.i("damn", "Highest after : " + baseFreq);
			 // if 8000/(2*256) = 15.625, if 44100/(2*32768) = 0.67
			//Log.i("damn", "Basnot-frekvens: " + /*baseFreq */ baseFreq*step+"\nBlockSize-newSpectra.length = " + (blockSize-newSpectra.length));
			//Log.i("freq", "amplitude : " + maxAmp2[1]);
			//String bb = String.format("%.2f", maxAmp2[1]*step);
			freqText.setText("Base frequency = " + baseFreq*step +
					" Hz\nTop amplitude at: " + (int)(maxAmp2[1]*step) +" Hz\nPoints: " + nrOfPoints);
			
			
			//Anv�nder "Top Amplitude" och l�gger till 1 po�ng om man �r +/-50Hz ifr�n
			
			
			/*if(((maxAmp2[1]*step+50)>=freqOfTone) && ((maxAmp2[1]*step-50)<=freqOfTone)){
				
				nrOfPoints++;
				
			}*/
			
			if(getPoints){
				increasePoints();
			}
			
			
			
			
			
			return (double) baseFreq * step;
	    
        }
        
        class FreqView extends TextView {
          
            public FreqView(Context ctx) {
                super(ctx);
                setText("Frequency: ");
            }
        }
        
        class ResetButton extends Button {

            OnClickListener clicker = new OnClickListener() {
                public void onClick(View v) {
                    newSpectra = new double[blockSize];
                    maxAmp2 = new double[2];
                    
                    freqText.setText("Base frequency = 0 Hz\nTop amplitude at: 0 Hz\nPoints: ");
                }
            };

            public ResetButton(Context ctx) {
                super(ctx);
                setText("Reset base frequency");
                setOnClickListener(clicker);
            }
        }
        
       /* private void playSound(){
        	
    		final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
    				AudioFormat.CHANNEL_OUT_MONO,
        			AudioFormat.ENCODING_PCM_16BIT, numSamples, AudioTrack.MODE_STATIC);
    		audioTrack.write(buffer2, 0, sample.length);
    		audioTrack.play();
        	
        }*/
        
        private void genFreq(){
        	boolean Kalle = true;
        	while(Kalle){
        		Random r = new Random();
        		freqOfTone = r.nextInt(maxFreqGen - minFreqGen + 1) + minFreqGen;
        		if(freqOfTone % 100 == 0){
        			Kalle = false;
        		}
        	}        	
        }

        
        
        private void genTone(){
        	
        	        	
    		for(int i = 0; i < 15*8*1024; i++){
    			sample[i] = Math.sin(x * i / (44100/freqOfTone));
    			buffer2[i] = (short) (sample[i] * Short.MAX_VALUE);
    		}
    		
    	
    		
    		final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
    				AudioFormat.CHANNEL_OUT_MONO,
        			AudioFormat.ENCODING_PCM_16BIT, (15*8*44100), AudioTrack.MODE_STATIC);
    		
    		audioTrack.write(buffer2, 0, buffer2.length);
    		audioTrack.play();
    		Log.i("BAJBAJS", "hhejj");
    		
    	
    		//int idx = 0;
    		/*for(final double dVal : sample){
    			
    			final short val = (short)((dVal * 32767));
    			
    			generatedSnd[idx++] = (byte) (val & 0x00ff);
    			generatedSnd[idx++] = (byte) ((val & 0xff0) >>> 8);
    			
    		}*/
    		
    		
    	}
        
        private void increasePoints(){
        	
        	
        	if(((maxAmp2[1]*step+50)>=freqOfTone) && ((maxAmp2[1]*step-50)<=freqOfTone)){
				
				nrOfPoints++;
				
				getPoints = false;
				
			}
        	
        }
      
}


    
