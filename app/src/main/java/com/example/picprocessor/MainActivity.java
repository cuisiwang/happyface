package com.example.picprocessor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {


    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private ImageView myImageView;//通过ImageView来显示结果
    private Bitmap selectbp;//所选择的bitmap
    private int flag=0;
    private View.OnTouchListener listener;

    private float f_x=0,f_y=0;
    private float L_x=0,L_y=0;//裁剪图片所用四个坐标点

    private int lisenterEnable=0;

    //相机权限获取
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,//写权限
            Manifest.permission.CAMERA//照相权限
    };


    //初始化
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        staticLoadCVLibraries();//静态注册opencv
        //用于判断SDK版本是否大于23
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            //检查权限
            int i = ContextCompat.checkSelfPermission(this,PERMISSIONS_STORAGE[0]);
            //如果权限申请失败，则重新申请权限
            if(i!= PackageManager.PERMISSION_GRANTED){
                //重新申请权限函数
                startRequestPermission();
                Log.e("这里","权限请求成功");
            }
        }

        myImageView = (ImageView)findViewById(R.id.imageView);
        myImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);//设置显示图片的属性。把图片按比例扩大/缩小到View的宽度，居中显示

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//允许用户选择特殊种类的数据，并返回（特殊种类的数据：照一张相片或录一段音）
        startActivityForResult(Intent.createChooser(intent,"选择图像..."), PICK_IMAGE_REQUEST);//启动另外一个活动

        FloatingActionButton close =findViewById(R.id.close);//返回重新选按钮
        close.setOnClickListener(view -> {
            finish();
        });


        FloatingActionButton save=findViewById(R.id.save);//保存
        save.setOnClickListener(view -> {
            Bitmap bitmap = ((BitmapDrawable)myImageView.getDrawable()).getBitmap();
            MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "HappyFace"+new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis())),"");
            Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
        });

        //定义处理的按钮
        Button processBtn = (Button)findViewById(R.id.process_btn);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                convertGray();//灰度转换
            }

            //灰度转换函数
            private void convertGray() {
                Mat src = new Mat();
                Mat temp = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成

                Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
                Log.i("CV", "image type:" + (temp.type() == CvType.CV_8UC3));
                Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);//灰度化处理。

                Bitmap selectbp1 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp1);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp1);//显示位图
            }
        });

        //模糊
        //定义处理的按钮
        Button processBtn_blur = (Button)findViewById(R.id.process_btn_blur);
        processBtn_blur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                BlurProcess();//灰度转换
            }

            //灰度转换函数
            private void BlurProcess() {
                Mat src = new Mat();
                Mat temp = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
                Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）

                Imgproc.blur(temp,dst,new Size(20,20));//调整这个size数值就可以改变效果
                Bitmap selectbp2 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp2);//显示位图
            }
        });

        //二值化
        //定义处理的按钮
        Button processBtn_Binarization = (Button)findViewById(R.id.Binarization);
        processBtn_Binarization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                BinarizationProcess();//二值化
            }

            //灰度转换函数
            private void BinarizationProcess() {
                Mat src = new Mat();
                Mat temp = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
                Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
                Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);//灰度化处理。

                Imgproc.threshold(temp,dst,50,255,Imgproc.THRESH_BINARY);
                Bitmap selectbp2 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp2);//显示位图
            }
        });

        //canny
        //定义处理的按钮
        Button processBtn_canny = (Button)findViewById(R.id.canny);
        processBtn_canny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                cannyProcess();
            }

            //灰度转换函数
            private void cannyProcess() {
                Mat src = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
                Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
                //先进行高斯模糊
                Imgproc.GaussianBlur(src,src,new Size(3,3),0);

                Mat gray=new Mat();
                Mat edges=new Mat();
                //转换为灰度图
                Imgproc.cvtColor(src,gray,Imgproc.COLOR_BGR2GRAY);
                Imgproc.Canny(src,edges,50,150,3,true);
                Core.bitwise_and(src,src,dst,edges);

                Bitmap selectbp2 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp2);//显示位图
            }
        });


        //直方图
        //定义处理的按钮
        Button processBtn_histogram = (Button)findViewById(R.id.histogram);
        processBtn_histogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                // makeText(MainActivity.this.getApplicationContext(), "hello, image process", Toast.LENGTH_SHORT).show();
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                histogramProcess();
            }

            //处理
            private void histogramProcess() {
                Mat src = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
                Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
                //进行直方图绘制
                displayHistogram(src,dst);

                Bitmap selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp2);//显示位图
            }

            private void displayHistogram(Mat src, Mat dst){
                Mat gray=new Mat();
                Imgproc.cvtColor(src,gray,Imgproc.COLOR_BGR2GRAY);//转换为灰度图

                //计算直方图数据并归一化
                List<Mat> images=new ArrayList<>();
                images.add(gray);
                Mat mask=Mat.ones(src.size(),CvType.CV_8UC1);
                Mat hist=new Mat();
                Imgproc.calcHist(images,new MatOfInt(0),mask,hist,new MatOfInt(256),new MatOfFloat(0,255));
                Core.normalize(hist,hist,0,255,Core.NORM_MINMAX);
                int height=hist.rows();

                dst.create(400,400,src.type());
                dst.setTo(new Scalar(200,200,200));
                float[] histdata=new float[256];
                hist.get(0,0,histdata);
                int offsetx=50;
                int offsety=350;

                //绘制直方图
                Imgproc.line(dst,new Point(offsetx,0),new Point(offsetx,offsety),new Scalar(0,0,0));
                Imgproc.line(dst,new Point(offsetx,offsety),new Point(400,offsety),new Scalar(0,0,0));

                for(int i=0;i<height-1;i++){
                    int y1=(int) histdata[i];
                    int y2=(int) histdata[i+1];
                    Rect rect =new Rect();
                    rect.x=offsetx+i;
                    rect.y=offsety-y1;
                    rect.width=1;
                    rect.height=y1;
                    Imgproc.rectangle(dst,rect.tl(),rect.br(),new Scalar(15,15,15));
                }
                //释放内存
                gray.release();
            }
        });

        //直方图均衡
        //定义处理的按钮
        Button processBtn_histogram_equalization = (Button)findViewById(R.id.histogram_equalization);
        processBtn_histogram_equalization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                histogram_equalization_Process();
            }

            //处理
            private void histogram_equalization_Process() {
                Mat src = new Mat();
                Mat dst = new Mat();
                Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
                Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
                //灰度转换
                Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2GRAY);
                //进行直方图均衡
                Imgproc.equalizeHist(src,src);
                displayHistogram(src,dst);

                Bitmap selectbp2 = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(dst, selectbp2);//再将mat转换为位图
                myImageView.setImageBitmap(selectbp2);//显示位图
            }

            private void displayHistogram(Mat gray, Mat dst){

                //计算直方图数据并归一化
                List<Mat> images=new ArrayList<>();
                images.add(gray);
                Mat mask=Mat.ones(gray.size(),CvType.CV_8UC1);
                Mat hist=new Mat();
                Imgproc.calcHist(images,new MatOfInt(0),mask,hist,new MatOfInt(256),new MatOfFloat(0,255));
                Core.normalize(hist,hist,0,255,Core.NORM_MINMAX);
                int height=hist.rows();

                dst.create(400,400,gray.type());
                dst.setTo(new Scalar(200,200,200));
                float[] histdata=new float[256];
                hist.get(0,0,histdata);
                int offsetx=50;
                int offsety=350;

                //绘制直方图
                Imgproc.line(dst,new Point(offsetx,0),new Point(offsetx,offsety),new Scalar(0,0,0));
                Imgproc.line(dst,new Point(offsetx,offsety),new Point(400,offsety),new Scalar(0,0,0));

                for(int i=0;i<height-1;i++){
                    int y1=(int) histdata[i];
                    Rect rect =new Rect();
                    rect.x=offsetx+i;
                    rect.y=offsety-y1;
                    rect.width=1;
                    rect.height=y1;
                    Imgproc.rectangle(dst,rect.tl(),rect.br(),new Scalar(15,15,15));
                }
                //释放内存
                gray.release();
            }

        });

        //ROI提取
        //定义处理的按钮
        Button processBtn_roi = (Button)findViewById(R.id.process_btn_roi);
        processBtn_roi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(lisenterEnable==1){
                    Toast.makeText(MainActivity.this, "图片裁剪已关闭", Toast.LENGTH_SHORT).show();
                    lisenterEnable=0;
                    f_x=f_y=L_y=L_x=0;
                }else{
                    Toast.makeText(MainActivity.this, "进入图片裁剪，再次点击按钮取消", Toast.LENGTH_SHORT).show();
                    lisenterEnable=1;//启用listener，事件结束后会进行裁剪
                }
              //  myImageView.setOnTouchListener(null);
            }
        });

    }




    //免安装Opencv manager
    //onResume()这个方法在活动准备好和用户进行交互的时候调用。此时的活动一定位于返回栈的栈顶，并且处于运行状态。
    //所以在活动开启前调用，检查是否有opencv库，若没有，则下载
    @Override
    protected void onResume() {
        super.onResume();
        //免安装opencv manager（opencv3.0开始可以采用这种方法）
        if (!OpenCVLoader.initDebug()) {
            System.out.println("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            System.out.println("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    // OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    System.out.println("OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    //在一个主界面(主Activity)通过意图（startActivityForResult）跳转至多个不同子Activity上去，
    // 当子模块的代码执行完毕后再次返回主页面，将子activity中得到的数据显示在主界面/完成的数据交给主Activity处理。
    // 这种带数据的意图跳转需要使用activity的onActivityResult()方法
    //note:点击完选择图片按钮后，应该进入的是这里的选项
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //requestCode 最初提供给startActivityForResult（）的整数请求代码，允许您识别此结果的来源。
        //整数requestCode用于与startActivityForResult中的requestCode中值进行比较判断，是以便确认返回的数据是从哪个Activity返回的。
        //resultCode 子活动通过其setResult（）返回的整数结果代码。适用于多个activity都返回数据时，来标识到底是哪一个activity返回的值。
        //data。一个Intent对象，带有返回的数据。可以通过data.getXxxExtra( );方法来获取指定数据类型的数据，
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {//当为选择image这个意图时，进入下面代码。选择图片以位图的形式显示出来
            Uri uri = data.getData();
            try {
                Log.d("image-tag", "start to decode selected image now...");
                InputStream input = getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                int raw_width = options.outWidth;
                int raw_height = options.outHeight;
                int max = Math.max(raw_width, raw_height);
                int newWidth = raw_width;
                int newHeight = raw_height;
                int inSampleSize = 1;
                if(max > max_size) {
                    newWidth = raw_width / 2;
                    newHeight = raw_height / 2;
                    while((newWidth/inSampleSize) > max_size || (newHeight/inSampleSize) > max_size) {
                        inSampleSize *=2;
                    }
                }

                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                selectbp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);

                myImageView.setImageBitmap(selectbp);//将所选择的位图显示出来
                flag=1;
                selectbp.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput("picture.png", Context.MODE_PRIVATE));
                //建立图片点击监听
                final int[] time = {0};
                myImageView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && time[0]==0&&lisenterEnable==1) {
                           Toast.makeText(MainActivity.this, "点击图片裁剪区域1",Toast.LENGTH_SHORT).show();
                           f_x = motionEvent.getX();
                           f_y = motionEvent.getY();
                           time[0]++;
                           return true;
                       }
                       if (motionEvent.getAction() == MotionEvent.ACTION_DOWN&&time[0]==1&&lisenterEnable==1) {
                           Toast.makeText(MainActivity.this, "点击图片裁剪区域2", Toast.LENGTH_SHORT).show();
                           L_x = motionEvent.getX();
                           L_y = motionEvent.getY();
                           getRoI();//这个listener是为了裁剪函数（getRoI）设置

                           lisenterEnable=0;
                           f_x=f_y=L_y=L_x=0;
                           time[0]=0;
                           return true;
                   }
                        view.performClick();
                        return false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //灰度转换函数
    private void getRoI() {
        Mat src = new Mat();
        Utils.bitmapToMat(selectbp, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
        Rect rect = new Rect((int)f_x, (int)f_y,(int)L_x, (int)L_y); // 设置矩形ROI的位置
        //****************************************************************************************
        //注意：此处需要修改裁剪逻辑解决分辨率带来的无法正确裁剪问题。
        //思路是由绝对位置改为相对位置，再裁剪
        //****************************************************************************************
        Mat imgRectROI= new Mat(src, rect);      // 从原图中截取图片
        selectbp.recycle();
        selectbp = Bitmap.createBitmap(imgRectROI.width(), imgRectROI.height(), Bitmap.Config.ARGB_8888) ;
        //此处永久改变了原位图
        Utils.matToBitmap(imgRectROI, selectbp);//再将mat转换为位图
        myImageView.setImageBitmap(selectbp);//显示位图
        f_x=f_y=L_x=L_y=0;
    }
    private void startRequestPermission(){
        //321为请求码
        ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,321);
    }

}
