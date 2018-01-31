package com.example.hoangdang.diemdanh.currentSessionImage;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hoangdang.diemdanh.R;
import com.example.hoangdang.diemdanh.SupportClass.AppVariable;
import com.example.hoangdang.diemdanh.SupportClass.DatabaseHelper;
import com.example.hoangdang.diemdanh.SupportClass.Network;
import com.example.hoangdang.diemdanh.SupportClass.SecurePreferences;
import com.example.hoangdang.diemdanh.SupportClass.Student;
import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MyFaceDetect extends AppCompatActivity {

    @BindView(R.id.faceverify)
    Button verify;
    private KairosListener verifylistener;
    private Kairos kairos;

    @BindView(R.id.imageView)
    ImageView imageview;
    private String galleryID;
    private static final int RC_CAMERA_PERMISSION = 100;
    private static final int RC_CAMERA_VERIFY = 102;

    public static Socket socket;
    public boolean isOffline;
    public static int courseID;
    public static int attendanceID;
    public static int classHasCourseID;
    public static int user_role;
    public static int classID;
    public SharedPreferences prefs;

    Bitmap image;
    public DatabaseHelper db;
    ArrayList<Student> studentDBList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_face_detect);

        ButterKnife.bind(this);
        kairos = new Kairos();
        kairos.setAuthentication(this, getString(R.string.app_id), getString(R.string.api_key));


        galleryID = "14ctt";

        db = new DatabaseHelper(this);
        prefs = new SecurePreferences(this);

        isOffline = prefs.getInt(AppVariable.CURRENT_IS_OFFLINE, 0) == 1;
        courseID = prefs.getInt(AppVariable.CURRENT_COURSE_ID, 0);
        attendanceID = prefs.getInt(AppVariable.CURRENT_ATTENDANCE, 0);
        classHasCourseID = prefs.getInt(AppVariable.CURRENT_CLASS_HAS_COURSE_ID, 0);
        classID = prefs.getInt(AppVariable.CURRENT_CLASS_ID, 0);
        user_role = prefs.getInt(AppVariable.USER_ROLE, 0);

        // TODO: if offline
        if (Network.isOnline(this) && !isOffline){
            setSocket();
        }
        Log.wtf("Hiep2",removeAccent("Tô Bạch Tùng Hiệp"));
        studentDBList = db.getHiep();
        for(int i=0;i<studentDBList.size();i++){
            studentDBList.get(i).strName = removeAccent(studentDBList.get(i).strName);
            Log.wtf("Hiep",studentDBList.get(i).strName + " " + studentDBList.get(i).iID );
        }

        // Hiep - 178
        verifylistener = new KairosListener() {
            @Override
            public void onSuccess(String s) {
                try {
                    final ArrayList<DetectedPerson> mylist = new ArrayList<DetectedPerson>();
                    int x=0,y=0,height=0;
                    String temp = "";
                    String name = "";
                    JSONObject response = new JSONObject(s);
                    //Toast.makeText(MyFaceDetect.this,response.toString(),Toast.LENGTH_SHORT).show();
                    Log.d("Hiep",response.toString());
                    //Log.d("Testing", String.valueOf(response.getJSONArray("images").length()));
                    image = image.copy(Bitmap.Config.ARGB_8888,true);
                    Canvas c = new Canvas(image);
                    Paint myPaint = new Paint();
                    if(response.toString().contains("Errors"))
                    {
                        Toast.makeText(MyFaceDetect.this,"Error Found !! " + response.getJSONArray("Errors").getJSONObject(0).getString("Message") ,Toast.LENGTH_LONG).show();
                        Log.d("Hiep", String.valueOf(response.getJSONArray("Errors").getJSONObject(0).getString("Message")));
                        imageview.setImageBitmap(image);
                        return;
                    }
                    for(int i=0;i<response.getJSONArray("images").length();i++) {
                        if (response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getString("status").equals("success")) {
                            JSONArray array = response.getJSONArray("images").getJSONObject(i).getJSONArray("candidates");
                            Log.d("Hiep", array.toString());
                            temp += response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getString("subject_id") + " ";
                            x = response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getInt("topLeftX");
                            y = response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getInt("topLeftY");
                            height = response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getInt("height");
                            name = response.getJSONArray("images").getJSONObject(i).getJSONObject("transaction").getString("subject_id");
//                            if (array != null && array.length() > 0) {
//                                info.setText("Matched!");
//                            }
                            //c.setMatrix(imageview.getImageMatrix());
                            myPaint.setStyle(Paint.Style.FILL);
                            myPaint.setColor(Color.GREEN);
                            c.drawText(name,x-height/8,y,myPaint);
                            myPaint.setTextSize(8);
                            myPaint.setStyle(Paint.Style.STROKE);
                            //myPaint.setColor(Color.rgb(0, 0, 0));
                            myPaint.setStrokeWidth(1);
                            c.drawRect(x, y, x+height, y+height, myPaint);
                            mylist.add(new DetectedPerson(name,x,y,height));
                        }
                    }
                    if(mylist.size() == 0)
                    {
                        Toast.makeText(MyFaceDetect.this,"Found face but not recognize ! Please try again !",Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(MyFaceDetect.this,"Face found ! Please click on the face to check attendance !",Toast.LENGTH_SHORT).show();
                    image = image.copy(Bitmap.Config.RGB_565,true);
                    imageview.setImageBitmap(image);
                    imageview.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {

                            //Toast.makeText(MainActivity.this,String.valueOf(motionEvent.getRawX()) + " " + String.valueOf(motionEvent.getRawY()),Toast.LENGTH_SHORT).show();
                            Matrix inverse = new Matrix();
                            imageview.getImageMatrix().invert(inverse);
                            float[] pts = {
                                    motionEvent.getX(), motionEvent.getY()
                            };
                            inverse.mapPoints(pts);
                            double xtouch = Math.floor(pts[0]);
                            double ytouch = Math.floor(pts[1]);
                            for(int i=0;i<mylist.size();i++)
                            {
                                if(xtouch > mylist.get(i).x && xtouch < mylist.get(i).x + mylist.get(i).height && ytouch > mylist.get(i).y && ytouch < mylist.get(i).y + mylist.get(i).height) {
                                    Toast.makeText(MyFaceDetect.this,"Da diem danh " + mylist.get(i).name, Toast.LENGTH_SHORT).show();
                                    Diemdanh(mylist.get(i).name);
                                }
                            }
                            return false;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(String s) {
                Toast.makeText(MyFaceDetect.this, s, Toast.LENGTH_SHORT).show();
            }
        };

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    RC_CAMERA_PERMISSION);
        }

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, RC_CAMERA_VERIFY);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_CAMERA_PERMISSION:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RC_CAMERA_VERIFY:
                    image = (Bitmap) data.getExtras().get("data");
                    try {
                        Toast.makeText(MyFaceDetect.this,"Verifying...Please wait",Toast.LENGTH_SHORT).show();
                        kairos.recognize(image, galleryID, null, null, null, null, verifylistener);
                    } catch (JSONException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    }
    class DetectedPerson
    {
        String name;
        int x,y,height;

        public DetectedPerson(String name, int x, int y, int height) {
            this.height = height;
            this.name = name;
            this.x = x;
            this.y = y;
        }


    }
    private void Diemdanh(String name)
    {
        int id = 0;
        for(int i=0;i<studentDBList.size();i++)
        {
            if(name.equals(studentDBList.get(i).strName))
                id = studentDBList.get(i).iID;
        }
        Log.wtf("Hiep","Name la " + name + "Id la " + id);
        int attendanceID = prefs.getInt(AppVariable.CURRENT_ATTENDANCE, 0);
        db.changeAttendanceStatus(id, attendanceID, AppVariable.ATTENDANCE_STATUS);
        if(Network.isOnline(MyFaceDetect.this) && !isOffline){
            SharedPreferences pref = new SecurePreferences(MyFaceDetect.this);
            new SyncTask().execute(pref.getString(AppVariable.USER_TOKEN, null), String.valueOf(attendanceID), String.valueOf(id));
        }
    }
    public class SyncTask extends AsyncTask<String, Void, Integer> {

        private Exception exception;
        private String strJsonResponse;

        @Override
        protected void onPreExecute() {}

        @Override
        protected Integer doInBackground(String... params) {
            try {
                URL url = new URL(Network.API_ATTENDANCE_CHECKLIST);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    JSONObject jsonUserData = new JSONObject();
                    jsonUserData.put("token", params[0]);
                    jsonUserData.put("student_id", params[2]);
                    jsonUserData.put("attendance_id", params[1]);
                    jsonUserData.put("attendance_type", 1);

                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //write
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(jsonUserData.toString());
                    writer.flush();

                    //check http response code
                    int status = connection.getResponseCode();
                    switch (status){
                        case HttpURLConnection.HTTP_OK:
                            //read response
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                            StringBuilder sb = new StringBuilder();
                            String line;

                            while ((line = bufferedReader.readLine()) != null) {
                                line = line + "\n";
                                sb.append(line);
                            }

                            bufferedReader.close();
                            strJsonResponse = sb.toString();

                            return HttpURLConnection.HTTP_OK;
                        default:
                            exception = new Exception(connection.getResponseMessage());
                            return 0;
                    }
                }
                finally{
                    connection.disconnect();
                }
            }
            catch(Exception e) {
                exception = e;
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            JSONObject payload = new JSONObject();
            try {
                payload.put("course_id", courseID);
                payload.put("class_id", classID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("checkAttendanceUpdated", payload);
        }
    }
    private void setSocket() {
        try {
            socket = IO.socket(Network.HOST);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("socket_log", "connected");
            }

        }).on("checkAttendanceStopped", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject)args[0];
                int course_id;
                int class_id;
                try {
                    course_id = obj.getInt("course_id");
                    class_id = obj.getInt("class_id");

                    if (prefs.getInt(AppVariable.CURRENT_COURSE_ID, 0) == course_id &
                            prefs.getInt(AppVariable.CURRENT_CLASS_ID, 0) == class_id){
                        prefs.edit().putInt(AppVariable.CURRENT_ATTENDANCE, 0).apply();
                        socket.disconnect();
                        showClosedDialog(obj.getString("message"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
            }

        });
        socket.connect();
    }
    private void showClosedDialog(String mes) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.DialogeTheme)
                .setTitle("Attendance stopped")
                .setMessage(mes)
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        dialog.setCancelable(false);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });


        new Thread() {
            public void run() {
                MyFaceDetect.this.runOnUiThread(new Runnable(){

                    @Override
                    public void run(){
                        dialog.show();
                    }
                });
            }
        }.start();
    }
    public static String removeAccent(String s) {
        s = s.replace("Đ","D");
        s = s.replace("đ","d");
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }
}
