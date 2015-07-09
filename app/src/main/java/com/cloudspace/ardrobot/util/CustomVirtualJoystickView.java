package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ros.android.android_15.R.id;
import org.ros.android.android_15.R.layout;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.Vector3;

import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Twist;
import nav_msgs.Odometry;

public class CustomVirtualJoystickView extends RelativeLayout implements AnimationListener, MessageListener<Odometry>, NodeMain {
    private static final float BOX_TO_CIRCLE_RATIO = 1.363636F;
    private float magnetTheta = 10.0F;
    private static final float ORIENTATION_TACK_FADE_RANGE = 40.0F;
    private static final long TURN_IN_PLACE_CONFIRMATION_DELAY = 200L;
    private static final float FLOAT_EPSILON = 0.0010F;
    private static final float THUMB_DIVET_RADIUS = 16.5F;
    private static final float POST_LOCK_MAGNET_THETA = 20.0F;
    private static final int INVALID_POINTER_ID = -1;
    private Publisher<Twist> publisher;
    private RelativeLayout mainLayout;
    private ImageView intensity;
    private ImageView thumbDivet;
    private ImageView lastVelocityDivet;
    private ImageView[] orientationWidget;
    private TextView magnitudeText;
    private float contactTheta;
    private float normalizedMagnitude;
    private float contactRadius;
    private float deadZoneRatio = 0.0F;
    private float joystickRadius = 0.0F;
    private float parentSize = 0.0F;
    private float normalizingMultiplier;
    private ImageView currentRotationRange;
    private ImageView previousRotationRange;
    private volatile boolean turnInPlaceMode;
    private float turnInPlaceStartTheta = 0.0F;
    private float rightTurnOffset;
    private volatile float currentOrientation;
    private int pointerId = -1;
    private Point contactUpLocation;
    private boolean previousVelocityMode;
    private boolean magnetizedXAxis;
    private boolean holonomic;
    private volatile boolean publishVelocity;
    private Timer publisherTimer;
    private Vector3 currentlVelocityAngular, currentlVelocityLinear;
    private String topicName;
    private String nodeName;

    public CustomVirtualJoystickView(Context context) {
        super(context);
        initVirtualJoystick(context);
        topicName = "~cmd_vel";
    }

    public CustomVirtualJoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVirtualJoystick(context);
        topicName = "~cmd_vel";
    }

    public CustomVirtualJoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        topicName = "~cmd_vel";
    }

    public void setHolonomic(boolean enabled) {
        holonomic = enabled;
    }

    public void onAnimationEnd(Animation animation) {
        contactRadius = 0.0F;
        normalizedMagnitude = 0.0F;
        updateMagnitudeText();
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onNewMessage(Odometry message) {
        double w = message.getPose().getPose().getOrientation().getW();
        double x = message.getPose().getPose().getOrientation().getX();
        double y = message.getPose().getPose().getOrientation().getZ();
        double z = message.getPose().getPose().getOrientation().getY();
        double heading = Math.atan2(2.0D * y * w - 2.0D * x * z, x * x - y * y - z * z + w * w) * 180.0D / 3.141592653589793D;
        currentOrientation = (float) (-heading);
        if (turnInPlaceMode) {
            post(new Runnable() {
                public void run() {
                    updateTurnInPlaceRotation();
                }
            });
            postInvalidate();
        }

    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action & 255) {
            case 0:
                pointerId = event.getPointerId(event.getActionIndex());
                onContactDown();
                if (inLastContactRange(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()))) {
                    previousVelocityMode = true;
                    onContactMove((float) contactUpLocation.x + joystickRadius, (float) contactUpLocation.y + joystickRadius);
                } else {
                    onContactMove(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                }
                break;
            case 1:
            case 6:
                if ((action & '\uff00') >> 8 == pointerId) {
                    onContactUp();
                }
                break;
            case 2:
                if (pointerId != -1) {
                    if (previousVelocityMode) {
                        if (inLastContactRange(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()))) {
                            onContactMove((float) contactUpLocation.x + joystickRadius, (float) contactUpLocation.y + joystickRadius);
                        } else {
                            previousVelocityMode = false;
                        }
                    } else {
                        onContactMove(event.getX(event.findPointerIndex(pointerId)), event.getY(event.findPointerIndex(pointerId)));
                    }
                }
            case 3:
            case 4:
            case 5:
        }

        return true;
    }

    public void EnableSnapping() {
        magnetTheta = 10.0F;
    }

    public void DisableSnapping() {
        magnetTheta = 1.0F;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mainLayout.getWidth() != mainLayout.getHeight()) {
            setOnTouchListener((OnTouchListener) null);
        }

        parentSize = (float) mainLayout.getWidth();
        if (parentSize < 200.0F || parentSize > 400.0F) {
            setOnTouchListener((OnTouchListener) null);
        }

        joystickRadius = (float) (mainLayout.getWidth() / 2);
        normalizingMultiplier = 1.363636F / (parentSize / 2.0F);
        deadZoneRatio = 16.5F * normalizingMultiplier;
        magnitudeText.setTextSize(parentSize / 12.0F);
    }

    private void animateIntensityCircle(float endScale) {
        AnimationSet intensityCircleAnimation = new AnimationSet(true);
        intensityCircleAnimation.setInterpolator(new LinearInterpolator());
        intensityCircleAnimation.setFillAfter(true);
        RotateAnimation rotateAnim = new RotateAnimation(contactTheta, contactTheta, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(0L);
        rotateAnim.setFillAfter(true);
        intensityCircleAnimation.addAnimation(rotateAnim);
        ScaleAnimation scaleAnim = new ScaleAnimation(contactRadius, endScale, contactRadius, endScale, joystickRadius, joystickRadius);
        scaleAnim.setDuration(0L);
        scaleAnim.setFillAfter(true);
        intensityCircleAnimation.addAnimation(scaleAnim);
        intensity.startAnimation(intensityCircleAnimation);
    }

    private void animateIntensityCircle(float endScale, long duration) {
        AnimationSet intensityCircleAnimation = new AnimationSet(true);
        intensityCircleAnimation.setInterpolator(new LinearInterpolator());
        intensityCircleAnimation.setFillAfter(true);
        intensityCircleAnimation.setAnimationListener(this);
        RotateAnimation rotateAnim = new RotateAnimation(contactTheta, contactTheta, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(duration);
        rotateAnim.setFillAfter(true);
        intensityCircleAnimation.addAnimation(rotateAnim);
        ScaleAnimation scaleAnim = new ScaleAnimation(contactRadius, endScale, contactRadius, endScale, joystickRadius, joystickRadius);
        scaleAnim.setDuration(duration);
        scaleAnim.setFillAfter(true);
        intensityCircleAnimation.addAnimation(scaleAnim);
        intensity.startAnimation(intensityCircleAnimation);
    }

    private void animateOrientationWidgets() {
        for (int i = 0; i < orientationWidget.length; ++i) {
            float deltaTheta = differenceBetweenAngles((float) (i * 15), contactTheta);
            if (deltaTheta < 40.0F) {
                orientationWidget[i].setAlpha(1.0F - deltaTheta / 40.0F);
            } else {
                orientationWidget[i].setAlpha(0.0F);
            }
        }

    }

    private float differenceBetweenAngles(float angle0, float angle1) {
        return Math.abs((angle0 + 180.0F - angle1) % 360.0F - 180.0F);
    }

    private void endTurnInPlaceRotation() {
        turnInPlaceMode = false;
        currentRotationRange.setAlpha(0.0F);
        previousRotationRange.setAlpha(0.0F);
        intensity.setAlpha(1.0F);
    }

    private void initVirtualJoystick(Context context) {
        setGravity(17);
        LayoutInflater.from(context).inflate(layout.virtual_joystick, this, true);
        mainLayout = (RelativeLayout) findViewById(id.virtual_joystick_layout);
        magnitudeText = (TextView) findViewById(id.magnitude);
        intensity = (ImageView) findViewById(id.intensity);
        thumbDivet = (ImageView) findViewById(id.thumb_divet);
        orientationWidget = new ImageView[24];
        orientationWidget[0] = (ImageView) findViewById(id.widget_0_degrees);
        orientationWidget[1] = (ImageView) findViewById(id.widget_15_degrees);
        orientationWidget[2] = (ImageView) findViewById(id.widget_30_degrees);
        orientationWidget[3] = (ImageView) findViewById(id.widget_45_degrees);
        orientationWidget[4] = (ImageView) findViewById(id.widget_60_degrees);
        orientationWidget[5] = (ImageView) findViewById(id.widget_75_degrees);
        orientationWidget[6] = (ImageView) findViewById(id.widget_90_degrees);
        orientationWidget[7] = (ImageView) findViewById(id.widget_105_degrees);
        orientationWidget[8] = (ImageView) findViewById(id.widget_120_degrees);
        orientationWidget[9] = (ImageView) findViewById(id.widget_135_degrees);
        orientationWidget[10] = (ImageView) findViewById(id.widget_150_degrees);
        orientationWidget[11] = (ImageView) findViewById(id.widget_165_degrees);
        orientationWidget[12] = (ImageView) findViewById(id.widget_180_degrees);
        orientationWidget[13] = (ImageView) findViewById(id.widget_195_degrees);
        orientationWidget[14] = (ImageView) findViewById(id.widget_210_degrees);
        orientationWidget[15] = (ImageView) findViewById(id.widget_225_degrees);
        orientationWidget[16] = (ImageView) findViewById(id.widget_240_degrees);
        orientationWidget[17] = (ImageView) findViewById(id.widget_255_degrees);
        orientationWidget[18] = (ImageView) findViewById(id.widget_270_degrees);
        orientationWidget[19] = (ImageView) findViewById(id.widget_285_degrees);
        orientationWidget[20] = (ImageView) findViewById(id.widget_300_degrees);
        orientationWidget[21] = (ImageView) findViewById(id.widget_315_degrees);
        orientationWidget[22] = (ImageView) findViewById(id.widget_330_degrees);
        orientationWidget[23] = (ImageView) findViewById(id.widget_345_degrees);
        ImageView[] arr$ = orientationWidget;
        int len$ = arr$.length;

        int i$;
        ImageView tack;
        for (i$ = 0; i$ < len$; ++i$) {
            tack = arr$[i$];
            tack.setAlpha(0.0F);
            tack.setVisibility(View.INVISIBLE);
        }

        magnitudeText.setTranslationX((float) (40.0D * Math.cos((double) (90.0F + contactTheta) * 3.141592653589793D / 180.0D)));
        magnitudeText.setTranslationY((float) (40.0D * Math.sin((double) (90.0F + contactTheta) * 3.141592653589793D / 180.0D)));
        animateIntensityCircle(0.0F);
        contactTheta = 0.0F;
        animateOrientationWidgets();
        currentRotationRange = (ImageView) findViewById(id.top_angle_slice);
        previousRotationRange = (ImageView) findViewById(id.mid_angle_slice);
        currentRotationRange.setAlpha(0.0F);
        previousRotationRange.setAlpha(0.0F);
        lastVelocityDivet = (ImageView) findViewById(id.previous_velocity_divet);
        contactUpLocation = new Point(0, 0);
        holonomic = false;
        arr$ = orientationWidget;
        len$ = arr$.length;

        for (i$ = 0; i$ < len$; ++i$) {
            tack = arr$[i$];
            tack.setVisibility(View.INVISIBLE);
        }

    }

    private void onContactDown() {
        thumbDivet.setAlpha(1.0F);
        magnitudeText.setAlpha(1.0F);
        lastVelocityDivet.setAlpha(0.0F);
        ImageView[] arr$ = orientationWidget;
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            ImageView tack = arr$[i$];
            tack.setVisibility(View.VISIBLE);
        }

        publishVelocity = true;
    }

    private void onContactMove(float x, float y) {
        float thumbDivetX = x - joystickRadius;
        float thumbDivetY = y - joystickRadius;
        contactTheta = (float) (Math.atan2((double) thumbDivetY, (double) thumbDivetX) * 180.0D / 3.141592653589793D + 90.0D);
        contactRadius = (float) Math.sqrt((double) (thumbDivetX * thumbDivetX + thumbDivetY * thumbDivetY)) * normalizingMultiplier;
        normalizedMagnitude = (contactRadius - deadZoneRatio) / (1.0F - deadZoneRatio);
        if (contactRadius >= 1.0F) {
            thumbDivetX /= contactRadius;
            thumbDivetY /= contactRadius;
            normalizedMagnitude = 1.0F;
            contactRadius = 1.0F;
        } else if (contactRadius < deadZoneRatio) {
            thumbDivetX = 0.0F;
            thumbDivetY = 0.0F;
            normalizedMagnitude = 0.0F;
        }

        if (!magnetizedXAxis) {
            if ((contactTheta + 360.0F) % 90.0F < magnetTheta) {
                contactTheta -= (contactTheta + 360.0F) % 90.0F;
            } else if ((contactTheta + 360.0F) % 90.0F > 90.0F - magnetTheta) {
                contactTheta += 90.0F - (contactTheta + 360.0F) % 90.0F;
            }

            if (floatCompare(contactTheta, 90.0F) || floatCompare(contactTheta, 270.0F)) {
                magnetizedXAxis = true;
            }
        } else if (differenceBetweenAngles((contactTheta + 360.0F) % 360.0F, 90.0F) < 20.0F) {
            contactTheta = 90.0F;
        } else if (differenceBetweenAngles((contactTheta + 360.0F) % 360.0F, 270.0F) < 20.0F) {
            contactTheta = 270.0F;
        } else {
            magnetizedXAxis = false;
        }

        animateIntensityCircle(contactRadius);
        animateOrientationWidgets();
        updateThumbDivet(thumbDivetX, thumbDivetY);
        updateMagnitudeText();
        if (holonomic) {
            publishVelocity((double) normalizedMagnitude * Math.cos((double) contactTheta * 3.141592653589793D / 180.0D), (double) normalizedMagnitude * Math.sin((double) contactTheta * 3.141592653589793D / 180.0D), 0.0D);
        } else {
            publishVelocity((double) normalizedMagnitude * Math.cos((double) contactTheta * 3.141592653589793D / 180.0D), 0.0D, (double) normalizedMagnitude * Math.sin((double) contactTheta * 3.141592653589793D / 180.0D));
        }

        updateTurnInPlaceMode();
    }

    private void updateTurnInPlaceMode() {
        if (!turnInPlaceMode) {
            if (floatCompare(contactTheta, 270.0F)) {
                turnInPlaceMode = true;
                rightTurnOffset = 0.0F;
            } else {
                if (!floatCompare(contactTheta, 90.0F)) {
                    return;
                }

                turnInPlaceMode = true;
                rightTurnOffset = 15.0F;
            }

            initiateTurnInPlace();
            (new Timer()).schedule(new TimerTask() {
                public void run() {
                    post(new Runnable() {
                        public void run() {
                            if (turnInPlaceMode) {
                                currentRotationRange.setAlpha(1.0F);
                                previousRotationRange.setAlpha(1.0F);
                                intensity.setAlpha(0.2F);
                            }

                        }
                    });
                    postInvalidate();
                }
            }, 200L);
        } else if (!floatCompare(contactTheta, 270.0F) && !floatCompare(contactTheta, 90.0F)) {
            endTurnInPlaceRotation();
        }

    }

    private void onContactUp() {
        animateIntensityCircle(0.0F, (long) (normalizedMagnitude * 1000.0F));
        magnitudeText.setAlpha(0.4F);
        lastVelocityDivet.setTranslationX(thumbDivet.getTranslationX());
        lastVelocityDivet.setTranslationY(thumbDivet.getTranslationY());
        lastVelocityDivet.setAlpha(0.4F);
        contactUpLocation.x = (int) thumbDivet.getTranslationX();
        contactUpLocation.y = (int) thumbDivet.getTranslationY();
        updateThumbDivet(0.0F, 0.0F);
        pointerId = -1;
        publishVelocity(0.0D, 0.0D, 0.0D);
        publishVelocity = false;
//        publisher.publish(currentVelocityCommand);
        endTurnInPlaceRotation();
        ImageView[] arr$ = orientationWidget;
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            ImageView tack = arr$[i$];
            tack.setVisibility(View.INVISIBLE);
        }

    }

    private void publishVelocity(double linearVelocityX, double linearVelocityY, double angularVelocityZ) {
        currentlVelocityLinear = new Vector3(linearVelocityX, -linearVelocityY, 0.0D);
        currentlVelocityAngular = new Vector3(0.0D, 0.0D, -angularVelocityZ);

        if (publisher != null) {
            publisher.publish(generateCurrentVelocityCommand());
        } else {
            Translation t = CylonApiBridge.getInstance().getTranslatedApi(nodeName);
            if (t != null) {
                CylonApiBridge.getInstance().activateTranslatedApi(t, t.tI.translate(new Vector3[]{currentlVelocityLinear, currentlVelocityAngular}), getContext());
            }
        }
    }

    private void initiateTurnInPlace() {
        turnInPlaceStartTheta = (currentOrientation + 360.0F) % 360.0F;
        RotateAnimation rotateAnim = new RotateAnimation(rightTurnOffset, rightTurnOffset, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(0L);
        rotateAnim.setFillAfter(true);
        currentRotationRange.startAnimation(rotateAnim);
        rotateAnim = new RotateAnimation(15.0F, 15.0F, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(0L);
        rotateAnim.setFillAfter(true);
        previousRotationRange.startAnimation(rotateAnim);
    }

    private void updateMagnitudeText() {
        if (!turnInPlaceMode) {
            magnitudeText.setText((int) (normalizedMagnitude * 100.0F) + "%");
            magnitudeText.setTranslationX((float) ((double) (parentSize / 4.0F) * Math.cos((double) (90.0F + contactTheta) * 3.141592653589793D / 180.0D)));
            magnitudeText.setTranslationY((float) ((double) (parentSize / 4.0F) * Math.sin((double) (90.0F + contactTheta) * 3.141592653589793D / 180.0D)));
        }

    }

    private void updateTurnInPlaceRotation() {
        float currentTheta = (currentOrientation + 360.0F) % 360.0F;
        float offsetTheta = (turnInPlaceStartTheta - currentTheta + 360.0F) % 360.0F;
        offsetTheta = 360.0F - offsetTheta;
        magnitudeText.setText(String.valueOf((int) offsetTheta));
        offsetTheta = (float) ((int) (offsetTheta - offsetTheta % 15.0F));
        RotateAnimation rotateAnim = new RotateAnimation(offsetTheta + rightTurnOffset, offsetTheta + rightTurnOffset, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(0L);
        rotateAnim.setFillAfter(true);
        currentRotationRange.startAnimation(rotateAnim);
        rotateAnim = new RotateAnimation(offsetTheta + 15.0F, offsetTheta + 15.0F, joystickRadius, joystickRadius);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setDuration(0L);
        rotateAnim.setFillAfter(true);
        previousRotationRange.startAnimation(rotateAnim);
    }

    private void updateThumbDivet(float x, float y) {
        thumbDivet.setTranslationX(-16.5F);
        thumbDivet.setTranslationY(-16.5F);
        thumbDivet.setRotation(contactTheta);
        thumbDivet.setTranslationX(x);
        thumbDivet.setTranslationY(y);
    }

    private boolean floatCompare(float v1, float v2) {
        return Math.abs(v1 - v2) < 0.0010F;
    }

    private boolean inLastContactRange(float x, float y) {
        return Math.sqrt((double) ((x - (float) contactUpLocation.x - joystickRadius) * (x - (float) contactUpLocation.x - joystickRadius) + (y - (float) contactUpLocation.y - joystickRadius) * (y - (float) contactUpLocation.y - joystickRadius))) < 16.5D;
    }

    public void setTopicName(String topicName) {
        topicName = topicName;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("android_15/virtual_joystick_view");
    }

    public void onStart(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher(topicName, "geometry_msgs/Twist");
        Subscriber subscriber = connectedNode.newSubscriber("odom", "nav_msgs/Odometry");
        subscriber.addMessageListener(this);
        publisherTimer = new Timer();
        publisherTimer.schedule(new TimerTask() {
            public void run() {
                if (publishVelocity) {
                    publisher.publish(generateCurrentVelocityCommand());
                }

            }
        }, 0L, 80L);
    }

    private Twist generateCurrentVelocityCommand() {
        Twist res = publisher.newMessage();
        res.getLinear().setX(currentlVelocityLinear.getX());
        res.getLinear().setY(currentlVelocityLinear.getY());
        res.getLinear().setZ(currentlVelocityLinear.getZ());
        res.getAngular().setX(currentlVelocityAngular.getX());
        res.getAngular().setY(currentlVelocityAngular.getY());
        res.getAngular().setZ(currentlVelocityAngular.getZ());
        return res;
    }

    public void onShutdown(Node node) {
    }

    public void onShutdownComplete(Node node) {
        publisherTimer.cancel();
        publisherTimer.purge();
    }

    public void onError(Node node, Throwable throwable) {
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
