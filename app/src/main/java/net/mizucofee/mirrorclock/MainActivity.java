package net.mizucofee.mirrorclock;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> news = new ArrayList<>();

    @BindView(R.id.week)
    MirroredFontTextView weekTv;
    @BindView(R.id.date)
    MirroredFontTextView dateTv;
    @BindView(R.id.hour)
    MirroredFontTextView hourTv;
    @BindView(R.id.min)
    MirroredFontTextView minTv;
    @BindView(R.id.sec)
    MirroredFontTextView secTv;
    @BindView(R.id.coron)
    MirroredFontTextView coronTv;
    @BindView(R.id.marquee)
    MirroredFontTextView marqueeTv;
    private Animation animation;
    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        ButterKnife.bind(this);

        getNews();
        update();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!flag){}
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        animateText();
                    }
                });
            }
        }).start();
    }

    int now = 0;

    private void animateText(){
        WindowManager wm = getWindowManager();
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);

        animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1f,
                Animation.ABSOLUTE, size.x,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f
        );

        animation.setInterpolator(new LinearInterpolator());

        animation.setDuration(news.get(now).length() * 300);
        marqueeTv.setLayoutParams(new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().density * 36 * news.get(now).length()), LinearLayout.LayoutParams.WRAP_CONTENT));

        marqueeTv.setText(news.get(now));

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(isFirst){
                    isFirst = false;
                    animateText();
                    return;
                }
                isFirst = true;
                now++;
                if(news.size() <= now) {
                    now = 0;
                    marqueeTv.setVisibility(View.INVISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getNews();
                            while (!flag){}
                            new Handler(getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    marqueeTv.setVisibility(View.VISIBLE);
                                    animateText();
                                }
                            });
                        }
                    }).start();
                } else
                    animateText();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        marqueeTv.startAnimation(animation);
    }

    private void update(){
        final Handler h = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Date date = new Date(System.currentTimeMillis());
                            dateTv.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(date));
                            hourTv.setText(new SimpleDateFormat("HH", Locale.US).format(date));
                            minTv.setText(new SimpleDateFormat("mm", Locale.US).format(date));
                            secTv.setText(new SimpleDateFormat("ss", Locale.US).format(date));
                            coronTv.setText((Integer.parseInt(new SimpleDateFormat("ss", Locale.US).format(date)) & 1) == 0 ? ":" : " ");
                            weekTv.setText(new SimpleDateFormat("E", Locale.US).format(date));
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private boolean flag = false;
    private void getNews(){
        flag = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection connection = new URL("https://headlines.yahoo.co.jp/rss/all-dom.xml").openConnection();
                    connection.setReadTimeout(10000);
                    DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = dbfactory.newDocumentBuilder(); // DocumentBuilderインスタンス

                    Document doc = builder.parse(connection.getInputStream());

                    NodeList localNodeList =
                            ((Element) doc.getElementsByTagName("channel").item(0)).getElementsByTagName("item");

                    news.clear();
                    for (int i = 0;localNodeList.getLength() != i;i++){
                        Element elementItem = (Element) localNodeList.item(i);
                        Element elementItemName = (Element) elementItem.getElementsByTagName("title").item(0);
                        String n = elementItemName.getFirstChild().getNodeValue();
                        String name = n.substring(n.lastIndexOf("（"));
                        name = name.substring(1,name.length() - 1);
                        n = n.substring(0,n.lastIndexOf("（"));
                        news.add("◇" + name + "◇" + n);
                    }
                    flag = true;
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                    flag = true;
                }
            }
        }).start();

    }
}
