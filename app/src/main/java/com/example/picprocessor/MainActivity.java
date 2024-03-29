package com.example.picprocessor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private final int PICK_IMAGE_REQUEST = 1;
    private ImageView myImageView;//通过ImageView来显示结果
    private Bitmap originalBP;//所选照片原BP
    private Bitmap selectbp;//所绘制的bitmap
    private int flag=0;
    private SeekBar SB;
    private Dialog dialog;

    private float f_x=0,f_y=0;
    private float L_x=0,L_y=0;//裁剪图片所用四个坐标点

    private int lisenterEnable=0;

    private ColorMatrix hueM =new ColorMatrix();
    private ColorMatrix satM = new ColorMatrix();
    private ColorMatrix lumM = new ColorMatrix();

    private Button lumB;//亮度调节
    private Button satB;//亮度调节
    private Button hueB;//亮度调节

    private Button xpB;//相片滤镜
    private Button hjB;//怀旧滤镜
    private Button qsB;//去色滤镜

    //相机权限获取
    private static final String[] PERMISSIONS_STORAGE = {
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
            }
        }

        dialog=new AnimDialog(MainActivity.this,R.style.FullActivity);


        myImageView = (ImageView)findViewById(R.id.imageView);
        myImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);//设置显示图片的属性。把图片按比例扩大/缩小到View的宽度，居中显示
       // dialog.setContentView(myImageView);
        myImageView.setOnClickListener(view -> {
            dialog.show();
            dialog.setContentView(getImageView(new BitmapDrawable(getResources(),selectbp)));
        });


        SB=findViewById(R.id.SBar);//程度调节进度条

        Uri uri=getIntent().getData();
        if(uri==null){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);//允许用户选择特殊种类的数据，并返回（特殊种类的数据：照一张相片或录一段音）
            startActivityForResult(Intent.createChooser(intent,"选择图像..."), PICK_IMAGE_REQUEST);//启动另外一个活动
        }else {
            drawPicture(uri);
        }



        FloatingActionButton close =findViewById(R.id.close);//返回重新选按钮
        close.setOnClickListener(view -> finish());

        FloatingActionButton reset=findViewById(R.id.reset);
        reset.setOnClickListener(view -> {
            SB.setVisibility(View.INVISIBLE);
            selectbp=originalBP.copy(originalBP.getConfig(),true);
            myImageView.setImageBitmap(originalBP);
        });

        FloatingActionButton save=findViewById(R.id.save);//保存
        save.setOnClickListener(view -> {
            MediaStore.Images.Media.insertImage(getContentResolver(), selectbp, "HappyFace"+new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis())),"");
            Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
        });

        //定义处理的按钮
        Button processBtn = (Button)findViewById(R.id.process_btn);
        processBtn.setOnClickListener(v -> {//定义按钮监听器
            if(flag==0){
                Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                return;
            }
            SB.setVisibility(View.INVISIBLE);
            convertGray();//灰度转换
        });

        //模糊
        //定义处理的按钮
        Button processBtn_blur = (Button)findViewById(R.id.process_btn_blur);
        processBtn_blur.setOnClickListener(v -> {//定义按钮监听器
            SB.setOnSeekBarChangeListener(null);
            SB.setVisibility(View.VISIBLE);
            SB.setMax(50);
            SB.setProgress(0);
            if(flag==0){
                Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap priBP=selectbp.copy(selectbp.getConfig(),true);
            SB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if(i==0) return;
                    BlurProcess(i,priBP);//灰度转换
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

        });

        //canny处理
        Button processBtn_canny = (Button)findViewById(R.id.canny);
        processBtn_canny.setOnClickListener(v -> {//定义按钮监听器
            if(flag==0){
                Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                return;
            }
            SB.setVisibility(View.INVISIBLE);
            cannyProcess();
        });


        //直方图绘制
        //想删了
        /*Button processBtn_histogram = (Button)findViewById(R.id.histogram);
        processBtn_histogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                SB.setVisibility(View.INVISIBLE);
                histogramProcess();
            }

            //绘制函数
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

        //绘制直方图均衡
        Button processBtn_histogram_equalization = (Button)findViewById(R.id.histogram_equalization);
        processBtn_histogram_equalization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//定义按钮监听器
                if(flag==0){
                    Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                    return;
                }
                SB.setVisibility(View.INVISIBLE);
                histogram_equalization_Process();
            }

            //绘制函数
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

        });*/

        //ROI截取
        //再想想
        Button processBtn_roi = (Button)findViewById(R.id.process_btn_roi);
        processBtn_roi.setOnClickListener(v -> {//定义按钮监听器
            if(flag==0){
                Toast.makeText(MainActivity.this, "请先选择一张图片！", Toast.LENGTH_SHORT).show();
                return;
            }
            SB.setVisibility(View.INVISIBLE);
            myImageView.setImageBitmap(selectbp);
            if(lisenterEnable==1){
                Toast.makeText(MainActivity.this, "图片裁剪已关闭", Toast.LENGTH_SHORT).show();
                lisenterEnable=0;
                f_x=f_y=L_y=L_x=0;
            }else{
                Toast.makeText(MainActivity.this, "进入图片裁剪，再次点击按钮取消", Toast.LENGTH_SHORT).show();
                lisenterEnable=1;//启用listener，事件结束后会进行裁剪
            }
        });

        lumB=findViewById(R.id.lumB);//亮度调节
        lumB.setOnClickListener(view -> {
            SB.setOnSeekBarChangeListener(null);
            SB.setMax(255);
            SB.setProgress(127);
            myImageView.setImageBitmap(selectbp);
            SB.setVisibility(View.VISIBLE);
            Bitmap priBP=selectbp.copy(selectbp.getConfig(),true);
            SB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float degree =i*1.0f/127;
                    //仅处理亮度改变，后需整合
                    lumM.setScale( degree, degree, degree,1);
                    Bitmap nselectbp=Bitmap.createBitmap(selectbp.getWidth(),selectbp.getHeight(),Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(nselectbp);
                    Paint paint = new Paint();
                    ColorMatrix imageMatrix = new ColorMatrix();
                    imageMatrix.postConcat(lumM);
                    paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));
                    canvas.drawBitmap(priBP, 0, 0, paint);
                    setBP(nselectbp);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        });


        hueB=findViewById(R.id.hueB);//色调
        hueB.setOnClickListener(view -> {
            SB.setOnSeekBarChangeListener(null);
            SB.setMax(255);
            SB.setProgress(127);
            myImageView.setImageBitmap(selectbp);
            SB.setVisibility(View.VISIBLE);
            Bitmap priBP=selectbp.copy(selectbp.getConfig(),true);
            SB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float hue=(i - 127) * 1.0F / 127 * 180;
                    hueM.setRotate(0,hue);
                    hueM.setRotate(1,hue);
                    hueM.setRotate(2,hue);
                    Bitmap nselectbp=Bitmap.createBitmap(selectbp.getWidth(),selectbp.getHeight(),Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(nselectbp);
                    Paint paint = new Paint();
                    ColorMatrix imageMatrix = new ColorMatrix();
                    imageMatrix.postConcat(hueM);
                    paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));
                    canvas.drawBitmap(priBP, 0, 0, paint);
                    setBP(nselectbp);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        });


        satB=findViewById(R.id.satB);//饱和度
        satB.setOnClickListener(view -> {
            SB.setOnSeekBarChangeListener(null);
            SB.setMax(255);
            SB.setProgress(127);
            myImageView.setImageBitmap(selectbp);
            SB.setVisibility(View.VISIBLE);
            Bitmap priBP=selectbp.copy(selectbp.getConfig(),true);
            SB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float sat=i*1.0f/127;
                    satM.setSaturation(sat);
                    Bitmap nselectbp=Bitmap.createBitmap(selectbp.getWidth(),selectbp.getHeight(),Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(nselectbp);
                    Paint paint = new Paint();
                    ColorMatrix imageMatrix = new ColorMatrix();
                    imageMatrix.postConcat(satM);
                    paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));
                    canvas.drawBitmap(priBP, 0, 0, paint);
                    setBP(nselectbp);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        });

        xpB=findViewById(R.id.xiangpianB);
        xpB.setOnClickListener(view -> {
            //这里填图像颜色反转对应效果函数
            SB.setVisibility(View.INVISIBLE);
            float[] CM ={-1,0,0,1,1,0,-1,0,1,1,0,0,-1,1,1,0,0,0,1,0};
            dealColorMatrixEffect(CM);
        });

        qsB=findViewById(R.id.quseB);
        qsB.setOnClickListener(view -> {
            //这里填去色效果对应效果函数
            SB.setVisibility(View.INVISIBLE);
            float[] CM ={1.05f,1.05f,1.05f,0,-1,1.05f,1.05f,1.05f,0,-1,1.05f,1.05f,1.05f,0,-1,0,0,0,1,0};

            dealColorMatrixEffect(CM);
        });

        hjB=findViewById(R.id.huaijiuB);
        hjB.setOnClickListener(view -> {
            //这里填怀旧效果对应效果函数
            SB.setVisibility(View.INVISIBLE);
            float[] CM ={0.393f,0.769f,0.189f,0,0,0.349f,0.686f,0.168f,0,0,0.272f,0.534f,0.131f,0,0,0,0,0,1,0};
            dealColorMatrixEffect(CM);
        });
    }

    //效果处理各函数
    //=============================================================================================================================================
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
        setBP(selectbp1);
    }

    //模糊函数
    private void BlurProcess(int deg,Bitmap priBP) {
        Bitmap temBP=priBP.copy(priBP.getConfig(),true);
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(temBP, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）

        Imgproc.blur(temp,dst,new Size(deg,deg));//调整这个size数值就可以改变效果
        Bitmap selectbp1 = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888) ;
        Utils.matToBitmap(dst, selectbp1);//再将mat转换为位图
        setBP(selectbp1);
    }
    //canny处理函数
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
        setBP(selectbp2);//显示位图
    }

    //处理使用颜色矩阵可以实现的效果
    private void dealColorMatrixEffect(float[] CM) {
        Bitmap bitmap = Bitmap.createBitmap(selectbp.getWidth(), selectbp.getHeight(), Bitmap.Config.ARGB_8888);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(CM);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(selectbp, 0, 0, paint);
        myImageView.setImageBitmap(bitmap);
    }

    private void setBP(Bitmap bm){
        selectbp=bm;
        myImageView.setImageBitmap(selectbp);
    }

    //============================================================================================================================================
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
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                System.out.println("OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };


    //在一个主界面(主Activity)通过意图（startActivityForResult）跳转至多个不同子Activity上去，
    // 当子模块的代码执行完毕后再次返回主页面，将子activity中得到的数据显示在主界面/完成的数据交给主Activity处理。
    // 这种带数据的意图跳转需要使用activity的onActivityResult()方法
    //note:点击完选择图片按钮后，应该进入的是这里的选项
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //requestCode 最初提供给startActivityForResult（）的整数请求代码，允许您识别此结果的来源。
        //整数requestCode用于与startActivityForResult中的requestCode中值进行比较判断，是以便确认返回的数据是从哪个Activity返回的。
        //resultCode 子活动通过其setResult（）返回的整数结果代码。适用于多个activity都返回数据时，来标识到底是哪一个activity返回的值。
        //data。一个Intent对象，带有返回的数据。可以通过data.getXxxExtra( );方法来获取指定数据类型的数据，
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            drawPicture(uri);
        }else finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void drawPicture(Uri uri) {
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
            double max_size = 1024;
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
            originalBP=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
            myImageView.setImageBitmap(selectbp);//将所选择的位图显示出来
            selectbp.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput("picture.png", Context.MODE_PRIVATE));
            flag=1;
            //建立图片点击监听
            /*final int[] time = {0};
            myImageView.setOnTouchListener((view, motionEvent) -> {
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
            });*/
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }


    //ROI处理函数
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
        Bitmap selectbp2 = Bitmap.createBitmap(imgRectROI.width(), imgRectROI.height(), Bitmap.Config.ARGB_8888) ;
        Utils.matToBitmap(imgRectROI, selectbp2);//再将mat转换为位图
        myImageView.setImageBitmap(selectbp2);//显示位图
        f_x=f_y=L_x=L_y=0;//清空位置选择点
    }
    //设置大图imageView
    private ImageView getImageView(Drawable draw){
        ImageView imageView = new ImageView(this);

        //宽高
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageView.setOnClickListener(v-> dialog.dismiss());
        //imageView设置图片
       /* try {
            InputStream input = getContentResolver().openInputStream(uri);
            Drawable drawable=Drawable.createFromStream(input,uri.toString());
            imageView.setImageDrawable(drawable);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        imageView.setImageDrawable(draw);
        return imageView;

    }

    private void startRequestPermission(){
        //321为请求码
        ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,321);
    }

}
