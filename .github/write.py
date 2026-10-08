import os

os.makedirs("app/src/main/res/layout", exist_ok=True)
os.makedirs("app/src/main/res/values", exist_ok=True)
os.makedirs("app/src/main/res/xml", exist_ok=True)

with open("gradle.properties", "w") as f:
    f.write("android.useAndroidX=true\n")
    f.write("android.enableJetifier=true\n")
    f.write("org.gradle.jvmargs=-Xmx2048m\n")

xml = """<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <LinearLayout
        android:id="@+id/rootLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        <TextView android:id="@+id/tvCpu" android:text="CPU 0%"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <TextView android:id="@+id/tvRam" android:text="RAM 0%"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <TextView android:id="@+id/tvNet" android:text="NET 0%"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swAimlock" android:text="AIMLOCK"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swBoost" android:text="BOOST RAM"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swAntiShake" android:text="SENSITIVITY"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swFixRung" android:text="FIX RUNG"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swOptimize" android:text="TOI UU"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swHead" android:text="BAM DAU"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swAntiban" android:text="ANTIBAN"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Switch android:id="@+id/swBoostFps" android:text="BOOST FPS"
            android:textColor="#FFFFFF"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Spinner android:id="@+id/spGame"android:layout_width="match_parent" android:layout_height="52dp"/>
        <Button android:id="@+id/btnPickBg" android:text="DOI ANH"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Button android:id="@+id/btnResetBg" android:text="MAC DINH"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
        <Button android:id="@+id/btnOn" android:text="ON" android:textSize="18sp"
            android:layout_width="match_parent" android:layout_height="60dp"/>
        <Button android:id="@+id/btnOff" android:text="OFF" android:textSize="18sp"
            android:layout_width="match_parent" android:layout_height="60dp"/>
        <TextView android:id="@+id/tvStatus" android:text=""
            android:textColor="#666666"
            android:layout_width="match_parent" android:layout_height="wrap_content"/>
    </LinearLayout>
</ScrollView>
"""

with open("app/src/main/res/layout/activity_main.xml", "w") as f:
    f.write(xml)

print("Files written OK")
