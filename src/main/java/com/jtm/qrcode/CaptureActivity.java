/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jtm.qrcode;

import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.jtm.qrcode.R;
import com.jtm.qrcode.result.ResultHandler;
import com.jtm.qrcode.result.ResultHandlerFactory;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Example Capture Activity.
 * 
 * @author Justin Wetherell (phishman3579@gmail.com)
 */
public class CaptureActivity extends DecoderActivity {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES = EnumSet.of(ResultMetadataType.ISSUE_NUMBER, ResultMetadataType.SUGGESTED_PRICE,
            ResultMetadataType.ERROR_CORRECTION_LEVEL, ResultMetadataType.POSSIBLE_COUNTRY);
    
    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
    private static final String INTENT_SCAN = "com.jtm.qrcode.SCAN";
    private static final String INTENT_SCAN_RESULT = "SCAN_RESULT";
    private static final String INTENT_SCAN_RESULT_FORMAT = "SCAN_RESULT_FORMAT";
    private static final String INTENT_SCAN_RESULT_BYTES = "RESULT_BYTES";
    private static final String INTENT_SCAN_RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION";
    private static final String INTENT_SCAN_RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION";
    private static final String INTENT_SCAN_RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL";
    private static final String INTENT_SCAN_RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_";

    private TextView statusView = null;
    private View resultView = null;
    private boolean inScanMode = false;
    private boolean isForResult = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.capture);
        Log.v(TAG, "onCreate()");

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);

        inScanMode = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        
        Intent intent = getIntent();
        if (intent != null) {
        	String action = intent.getAction();
            if (INTENT_SCAN.equals(action)) {
            	isForResult = true;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	if(isForResult) {
        		setResult(RESULT_CANCELED);
        	}
            if (inScanMode)
                finish();
            else
                onResume();
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA){
            // Handle these events so they don't launch the Camera app
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode) {
        drawResultPoints(barcode, rawResult);

        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        
        if(isForResult){
        	handleDecodeExternally(rawResult, resultHandler, barcode);
        }else{
        	handleDecodeInternally(rawResult, resultHandler, barcode);
        }
    }

    @Override
	protected void showScanner() {
        inScanMode = true;
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
    }

    protected void showResults() {
        inScanMode = false;
        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);
    }

    // Put up our own UI for how to handle the decodBarcodeFormated contents.
    private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {
        onPause();
        showResults();

        ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon));
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

        TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());

        TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String formattedTime = formatter.format(new Date(rawResult.getTimestamp()));
        TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
        timeTextView.setText(formattedTime);

        TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
        View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(View.GONE);
        metaTextViewLabel.setVisibility(View.GONE);
        Map<ResultMetadataType, Object> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (Map.Entry<ResultMetadataType, Object> entry : metadata.entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(View.VISIBLE);
                metaTextViewLabel.setVisibility(View.VISIBLE);
            }
        }

        TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
        CharSequence displayContents = resultHandler.getDisplayContents();
        contentsTextView.setText(displayContents);
        // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
    }
    
	// Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {

      if (barcode != null) {
    	  viewfinderView.drawResultBitmap(barcode);
      }

      // Hand back whatever action they requested
      Intent intent = new Intent(getIntent().getAction());
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(INTENT_SCAN_RESULT, rawResult.toString());
      intent.putExtra(INTENT_SCAN_RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
      byte[] rawBytes = rawResult.getRawBytes();
      if (rawBytes != null && rawBytes.length > 0) {
    	  intent.putExtra(INTENT_SCAN_RESULT_BYTES, rawBytes);
      }
      Map<ResultMetadataType,?> metadata = rawResult.getResultMetadata();
      if (metadata != null) {
    	  if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
    		  intent.putExtra(INTENT_SCAN_RESULT_UPC_EAN_EXTENSION, metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
	      }
	      Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
	      if (orientation != null) {
	    	  intent.putExtra(INTENT_SCAN_RESULT_ORIENTATION, orientation.intValue());
	      }
	      String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
	      if (ecLevel != null) {
	    	  intent.putExtra(INTENT_SCAN_RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
	      }
	      @SuppressWarnings("unchecked")
	      Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
	      if (byteSegments != null) {
	    	  int i = 0;
	    	  for (byte[] byteSegment : byteSegments) {
	    		  intent.putExtra(INTENT_SCAN_RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
	    		  i++;
	    	  }
	      }
      }
      sendReplyMessage(R.id.return_scan_result, intent, DEFAULT_INTENT_RESULT_DURATION_MS);
    }
    
    private void sendReplyMessage(int id, Object arg, long delayMS) {
    	if (handler != null) {
    		Message message = Message.obtain(handler, id, arg);
    		if (delayMS > 0L) {
    			handler.sendMessageDelayed(message, delayMS);
    		} else {
    			handler.sendMessage(message);
    		}
    	}
    }
}
