.class public Leu/pokemmo/client/AndroidLauncher;
.super Lf/nx3;
.source "r8-map-id-21a863b15956229bafbacbd9400e9628b6470120c5fc77a80c1de31b2e861762"


# static fields
.field public static final MA0:Lf/a97;

.field public static Mj:Z

.field public static final X4:Lf/a97;

.field public static final YA1:Lf/xv7;

.field public static j61:Z


# instance fields
.field public volatile Vc0:I

.field public dP:Lf/cp7;

.field public final fQ0:Lf/k89;


# direct methods
.method static constructor <clinit>()V
    .registers 2

    .line 1
    const-class v0, Leu/pokemmo/client/AndroidLauncher;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 8
    .line 9
    const/4 v0, 0x0

    .line 10
    sput-boolean v0, Leu/pokemmo/client/AndroidLauncher;->j61:Z

    .line 11
    .line 12
    sput-boolean v0, Leu/pokemmo/client/AndroidLauncher;->Mj:Z

    .line 13
    .line 14
    new-instance v0, Lf/a97;

    .line 15
    .line 16
    const v1, 0x36ee80

    .line 17
    .line 18
    .line 19
    invoke-direct {v0, v1}, Lf/a97;-><init>(I)V

    .line 20
    .line 21
    .line 22
    sput-object v0, Leu/pokemmo/client/AndroidLauncher;->MA0:Lf/a97;

    .line 23
    .line 24
    new-instance v0, Lf/a97;

    .line 25
    .line 26
    const v1, 0x493e0

    .line 27
    .line 28
    .line 29
    invoke-direct {v0, v1}, Lf/a97;-><init>(I)V

    .line 30
    .line 31
    .line 32
    sput-object v0, Leu/pokemmo/client/AndroidLauncher;->X4:Lf/a97;

    .line 33
    .line 34
    return-void
.end method

.method public constructor <init>()V
    .registers 2

    .line 1
    invoke-direct {p0}, Lf/nx3;-><init>()V

    .line 2
    .line 3
    .line 4
    new-instance v0, Lf/k89;

    .line 5
    .line 6
    invoke-direct {v0}, Lf/x44;-><init>()V

    .line 7
    .line 8
    .line 9
    iput-object v0, p0, Leu/pokemmo/client/AndroidLauncher;->fQ0:Lf/k89;

    .line 10
    .line 11
    const/4 v0, 0x0

    .line 12
    iput v0, p0, Leu/pokemmo/client/AndroidLauncher;->Vc0:I

    .line 13
    .line 14
    return-void
.end method

.method public static O7(Lf/d89;)V
    .registers 10

    .line 1
    const/16 v0, 0x80

    .line 2
    .line 3
    iget-object v1, p0, Lf/d89;->cH1:Lf/lw7;

    .line 4
    .line 5
    invoke-virtual {v1, v0}, Lf/lw7;->gD1(I)Lf/f44;

    .line 6
    .line 7
    .line 8
    move-result-object v0

    .line 9
    iget v1, v0, Lf/f44;->VG:I

    .line 10
    .line 11
    iget v2, v0, Lf/f44;->Ew:I

    .line 12
    .line 13
    iget v3, v0, Lf/f44;->eM0:I

    .line 14
    .line 15
    iget v0, v0, Lf/f44;->dC1:I

    .line 16
    .line 17
    sget v4, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 18
    .line 19
    const/16 v5, 0x1f

    .line 20
    .line 21
    if-lt v4, v5, :cond_6e

    .line 22
    .line 23
    invoke-virtual {p0}, Lf/d89;->gM0()Landroid/view/WindowInsets;

    .line 24
    .line 25
    .line 26
    move-result-object p0

    .line 27
    if-eqz p0, :cond_6e

    .line 28
    .line 29
    const/4 v4, 0x0

    .line 30
    invoke-virtual {p0, v4}, Landroid/view/WindowInsets;->getRoundedCorner(I)Landroid/view/RoundedCorner;

    .line 31
    .line 32
    .line 33
    move-result-object v5

    .line 34
    const/4 v6, 0x1

    .line 35
    invoke-virtual {p0, v6}, Landroid/view/WindowInsets;->getRoundedCorner(I)Landroid/view/RoundedCorner;

    .line 36
    .line 37
    .line 38
    move-result-object v6

    .line 39
    const/4 v7, 0x3

    .line 40
    invoke-virtual {p0, v7}, Landroid/view/WindowInsets;->getRoundedCorner(I)Landroid/view/RoundedCorner;

    .line 41
    .line 42
    .line 43
    move-result-object v7

    .line 44
    const/4 v8, 0x2

    .line 45
    invoke-virtual {p0, v8}, Landroid/view/WindowInsets;->getRoundedCorner(I)Landroid/view/RoundedCorner;

    .line 46
    .line 47
    .line 48
    move-result-object p0

    .line 49
    if-eqz v5, :cond_37

    .line 50
    .line 51
    invoke-virtual {v5}, Landroid/view/RoundedCorner;->getRadius()I

    .line 52
    .line 53
    .line 54
    move-result v5

    .line 55
    goto :goto_38

    .line 56
    :cond_37
    const/4 v5, 0x0

    .line 57
    :goto_38
    if-eqz v6, :cond_3f

    .line 58
    .line 59
    invoke-virtual {v6}, Landroid/view/RoundedCorner;->getRadius()I

    .line 60
    .line 61
    .line 62
    move-result v6

    .line 63
    goto :goto_40

    .line 64
    :cond_3f
    const/4 v6, 0x0

    .line 65
    :goto_40
    if-eqz v7, :cond_47

    .line 66
    .line 67
    invoke-virtual {v7}, Landroid/view/RoundedCorner;->getRadius()I

    .line 68
    .line 69
    .line 70
    move-result v7

    .line 71
    goto :goto_48

    .line 72
    :cond_47
    const/4 v7, 0x0

    .line 73
    :goto_48
    if-eqz p0, :cond_4e

    .line 74
    .line 75
    invoke-virtual {p0}, Landroid/view/RoundedCorner;->getRadius()I

    .line 76
    .line 77
    .line 78
    move-result v4

    .line 79
    :cond_4e
    invoke-static {v5, v7}, Ljava/lang/Math;->max(II)I

    .line 80
    .line 81
    .line 82
    move-result p0

    .line 83
    invoke-static {v1, p0}, Ljava/lang/Math;->max(II)I

    .line 84
    .line 85
    .line 86
    move-result v1

    .line 87
    invoke-static {v5, v6}, Ljava/lang/Math;->max(II)I

    .line 88
    .line 89
    .line 90
    move-result p0

    .line 91
    invoke-static {v2, p0}, Ljava/lang/Math;->max(II)I

    .line 92
    .line 93
    .line 94
    move-result v2

    .line 95
    invoke-static {v6, v4}, Ljava/lang/Math;->max(II)I

    .line 96
    .line 97
    .line 98
    move-result p0

    .line 99
    invoke-static {v3, p0}, Ljava/lang/Math;->max(II)I

    .line 100
    .line 101
    .line 102
    move-result v3

    .line 103
    invoke-static {v7, v4}, Ljava/lang/Math;->max(II)I

    .line 104
    .line 105
    .line 106
    move-result p0

    .line 107
    invoke-static {v0, p0}, Ljava/lang/Math;->max(II)I

    .line 108
    .line 109
    .line 110
    move-result v0

    .line 111
    :cond_6e
    sput v1, Lf/p37;->Sx0:I

    .line 112
    .line 113
    sput v2, Lf/p37;->WV:I

    .line 114
    .line 115
    sput v3, Lf/p37;->jz0:I

    .line 116
    .line 117
    sput v0, Lf/p37;->wR:I

    .line 118
    .line 119
    return-void
.end method


# virtual methods
.method public final BS1()V
    .registers 1

    .line 1
    invoke-virtual {p0}, Landroid/app/Activity;->finishAndRemoveTask()V

    .line 2
    .line 3
    .line 4
    return-void
.end method

.method public final onActivityResult(IILandroid/content/Intent;)V
    .registers 6

    .line 1
    packed-switch p1, :pswitch_data_16

    .line 2
    .line 3
    .line 4
    goto :goto_12

    .line 5
    :pswitch_4
    invoke-virtual {p0}, Landroid/app/Activity;->finishAndRemoveTask()V

    .line 6
    .line 7
    .line 8
    goto :goto_12

    .line 9
    :pswitch_8
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 10
    .line 11
    new-instance v1, Lf/gu7;

    .line 12
    .line 13
    invoke-direct {v1, p2, p3}, Lf/gu7;-><init>(ILandroid/content/Intent;)V

    .line 14
    .line 15
    .line 16
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 17
    .line 18
    .line 19
    :goto_12
    invoke-super {p0, p1, p2, p3}, Lf/nx3;->onActivityResult(IILandroid/content/Intent;)V

    .line 20
    .line 21
    .line 22
    return-void

    .line 23
    :pswitch_data_16
    .packed-switch 0x4403733e
        :pswitch_8
        :pswitch_4
    .end packed-switch
.end method

.method public final onCreate(Landroid/os/Bundle;)V
    .registers 21

    # MonMMO-EX: the mod scanner (f/Kg0) lists a real folder on external storage, so a mod
    # bundled in assets is invisible to it. Put ours there once, before anything reads mods.
    invoke-static/range {p0 .. p0}, Leu/pokemmo/client/AndroidLauncher;->monmmoInstallBundledMod(Landroid/content/Context;)V

    .line 1
    move-object/from16 v1, p0

    .line 2
    .line 3
    sget-boolean v0, Lf/ms5;->X60:Z

    .line 4
    .line 5
    const/16 v2, 0x1c

    .line 6
    .line 7
    const/4 v3, 0x1

    .line 8
    if-eqz v0, :cond_17

    .line 9
    .line 10
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 11
    .line 12
    if-lt v0, v2, :cond_17

    .line 13
    .line 14
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 15
    .line 16
    .line 17
    move-result-object v0

    .line 18
    invoke-virtual {v0}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager$LayoutParams;

    .line 19
    .line 20
    .line 21
    move-result-object v0

    .line 22
    iput v3, v0, Landroid/view/WindowManager$LayoutParams;->layoutInDisplayCutoutMode:I

    .line 23
    .line 24
    :cond_17
    invoke-super/range {p0 .. p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    .line 25
    .line 26
    .line 27
    const-string v0, "USER-ID"

    .line 28
    .line 29
    new-instance v4, Lcom/google/firebase/FirebaseOptions$Builder;

    .line 30
    .line 31
    invoke-direct {v4}, Lcom/google/firebase/FirebaseOptions$Builder;-><init>()V

    .line 32
    .line 33
    .line 34
    const-string v5, "pokemmo-ee772"

    .line 35
    .line 36
    invoke-virtual {v4, v5}, Lcom/google/firebase/FirebaseOptions$Builder;->setProjectId(Ljava/lang/String;)Lcom/google/firebase/FirebaseOptions$Builder;

    .line 37
    .line 38
    .line 39
    move-result-object v4

    .line 40
    const-string v5, "1:102388458548:android:72646623b3f6f5cc0a5661"

    .line 41
    .line 42
    invoke-virtual {v4, v5}, Lcom/google/firebase/FirebaseOptions$Builder;->setApplicationId(Ljava/lang/String;)Lcom/google/firebase/FirebaseOptions$Builder;

    .line 43
    .line 44
    .line 45
    move-result-object v4

    .line 46
    const-string v5, "AIzaSyCro5oC85U5GvOvCPApaEWPvYoT9g1eqGo"

    .line 47
    .line 48
    invoke-virtual {v4, v5}, Lcom/google/firebase/FirebaseOptions$Builder;->setApiKey(Ljava/lang/String;)Lcom/google/firebase/FirebaseOptions$Builder;

    .line 49
    .line 50
    .line 51
    move-result-object v4

    .line 52
    invoke-virtual {v4}, Lcom/google/firebase/FirebaseOptions$Builder;->build()Lcom/google/firebase/FirebaseOptions;

    .line 53
    .line 54
    .line 55
    move-result-object v4

    .line 56
    invoke-static {v1}, Lcom/google/firebase/FirebaseApp;->getApps(Landroid/content/Context;)Ljava/util/List;

    .line 57
    .line 58
    .line 59
    move-result-object v5

    .line 60
    invoke-interface {v5}, Ljava/util/List;->isEmpty()Z

    .line 61
    .line 62
    .line 63
    move-result v5

    .line 64
    if-eqz v5, :cond_44

    .line 65
    .line 66
    invoke-static {v1, v4}, Lcom/google/firebase/FirebaseApp;->initializeApp(Landroid/content/Context;Lcom/google/firebase/FirebaseOptions;)Lcom/google/firebase/FirebaseApp;

    .line 67
    .line 68
    .line 69
    :cond_44
    invoke-static {}, Lcom/google/firebase/FirebaseApp;->getInstance()Lcom/google/firebase/FirebaseApp;

    .line 70
    .line 71
    .line 72
    move-result-object v4

    .line 73
    const/4 v5, 0x0

    .line 74
    invoke-virtual {v4, v5}, Lcom/google/firebase/FirebaseApp;->setDataCollectionDefaultEnabled(Z)V

    .line 75
    .line 76
    .line 77
    invoke-static {}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->getInstance()Lcom/google/firebase/crashlytics/FirebaseCrashlytics;

    .line 78
    .line 79
    .line 80
    move-result-object v4

    .line 81
    invoke-virtual {v4, v5}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->setCrashlyticsCollectionEnabled(Z)V

    .line 82
    .line 83
    .line 84
    invoke-virtual {v1, v5}, Landroid/app/Activity;->getPreferences(I)Landroid/content/SharedPreferences;

    .line 85
    .line 86
    .line 87
    move-result-object v4

    .line 88
    const/4 v6, 0x0

    .line 89
    invoke-interface {v4, v0, v6}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 90
    .line 91
    .line 92
    move-result-object v7

    .line 93
    if-eqz v7, :cond_64

    .line 94
    .line 95
    invoke-virtual {v7}, Ljava/lang/String;->isEmpty()Z

    .line 96
    .line 97
    .line 98
    move-result v8

    .line 99
    if-eqz v8, :cond_77

    .line 100
    .line 101
    :cond_64
    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;

    .line 102
    .line 103
    .line 104
    move-result-object v7

    .line 105
    invoke-virtual {v7}, Ljava/util/UUID;->toString()Ljava/lang/String;

    .line 106
    .line 107
    .line 108
    move-result-object v7

    .line 109
    invoke-interface {v4}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    .line 110
    .line 111
    .line 112
    move-result-object v4

    .line 113
    invoke-interface {v4, v0, v7}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    .line 114
    .line 115
    .line 116
    move-result-object v0

    .line 117
    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->apply()V

    .line 118
    .line 119
    .line 120
    :cond_77
    invoke-static {}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->getInstance()Lcom/google/firebase/crashlytics/FirebaseCrashlytics;

    .line 121
    .line 122
    .line 123
    move-result-object v0

    .line 124
    invoke-virtual {v0, v7}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->setUserId(Ljava/lang/String;)V

    .line 125
    .line 126
    .line 127
    sput-boolean v3, Leu/pokemmo/client/AndroidLauncher;->Mj:Z

    .line 128
    .line 129
    sget-object v0, Lf/jl0$a24;->W5:Lf/jl0$a24;

    .line 130
    .line 131
    sput-object v0, Lf/z30;->VW1:Lf/jl0$a24;

    .line 132
    .line 133
    sget-boolean v0, Leu/pokemmo/client/AndroidLauncher;->j61:Z

    .line 134
    .line 135
    if-eqz v0, :cond_9f

    .line 136
    .line 137
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 138
    .line 139
    const-string v2, "Temporary hotfix: Detected reused JVM, forcibly killing app."

    .line 140
    .line 141
    invoke-interface {v0, v2}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 142
    .line 143
    .line 144
    :try_start_8f
    invoke-static {}, Landroid/os/Process;->myPid()I

    .line 145
    .line 146
    .line 147
    move-result v0

    .line 148
    invoke-static {v0}, Landroid/os/Process;->killProcess(I)V
    :try_end_96
    .catch Ljava/lang/Exception; {:try_start_8f .. :try_end_96} :catch_97

    .line 149
    .line 150
    .line 151
    goto :goto_9b

    .line 152
    :catch_97
    move-exception v0

    .line 153
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    .line 154
    .line 155
    .line 156
    :goto_9b
    invoke-static {v5}, Ljava/lang/System;->exit(I)V

    .line 157
    .line 158
    .line 159
    return-void

    .line 160
    :cond_9f
    sput-boolean v3, Leu/pokemmo/client/AndroidLauncher;->j61:Z

    .line 161
    .line 162
    invoke-static {}, Lf/tv7;->Rz0()Lf/av4;

    .line 163
    .line 164
    .line 165
    move-result-object v0

    .line 166
    invoke-interface {v0}, Lf/av4;->Nk0()Lf/hn6;

    .line 167
    .line 168
    .line 169
    move-result-object v0

    .line 170
    check-cast v0, Lch/qos/logback/classic/LoggerContext;

    .line 171
    .line 172
    invoke-virtual {v0}, Lch/qos/logback/classic/LoggerContext;->reset()V

    .line 173
    .line 174
    .line 175
    new-instance v4, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;

    .line 176
    .line 177
    invoke-direct {v4}, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;-><init>()V

    .line 178
    .line 179
    .line 180
    invoke-virtual {v4, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 181
    .line 182
    .line 183
    const-string v7, "[%level %date{yyyy-MM-dd HH:mm:ss zzz}][%thread] %lo:%L - %m%n"

    .line 184
    .line 185
    invoke-virtual {v4, v7}, Lch/qos/logback/core/pattern/PatternLayoutEncoderBase;->setPattern(Ljava/lang/String;)V

    .line 186
    .line 187
    .line 188
    invoke-virtual {v4}, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;->start()V

    .line 189
    .line 190
    .line 191
    new-instance v7, Ljava/io/File;

    .line 192
    .line 193
    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    .line 194
    .line 195
    .line 196
    move-result-object v8

    .line 197
    const-string v9, "log/console.log"

    .line 198
    .line 199
    invoke-direct {v7, v8, v9}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 200
    .line 201
    .line 202
    invoke-virtual {v7}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    .line 203
    .line 204
    .line 205
    move-result-object v7

    .line 206
    sput-object v7, Lf/qz5;->UO0:Ljava/lang/String;

    .line 207
    .line 208
    new-instance v7, Ljava/io/File;

    .line 209
    .line 210
    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    .line 211
    .line 212
    .line 213
    move-result-object v8

    .line 214
    const-string v9, "log/mods.log"

    .line 215
    .line 216
    invoke-direct {v7, v8, v9}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 217
    .line 218
    .line 219
    invoke-virtual {v7}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    .line 220
    .line 221
    .line 222
    move-result-object v7

    .line 223
    sput-object v7, Lf/qz5;->uQ0:Ljava/lang/String;

    .line 224
    .line 225
    new-instance v7, Lch/qos/logback/core/FileAppender;

    .line 226
    .line 227
    invoke-direct {v7}, Lch/qos/logback/core/FileAppender;-><init>()V

    .line 228
    .line 229
    .line 230
    invoke-virtual {v7, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 231
    .line 232
    .line 233
    sget-object v8, Lf/qz5;->UO0:Ljava/lang/String;

    .line 234
    .line 235
    invoke-virtual {v7, v8}, Lch/qos/logback/core/FileAppender;->setFile(Ljava/lang/String;)V

    .line 236
    .line 237
    .line 238
    invoke-virtual {v7, v5}, Lch/qos/logback/core/FileAppender;->setAppend(Z)V

    .line 239
    .line 240
    .line 241
    invoke-virtual {v7, v4}, Lch/qos/logback/core/OutputStreamAppender;->setEncoder(Lch/qos/logback/core/encoder/Encoder;)V

    .line 242
    .line 243
    .line 244
    invoke-virtual {v7}, Lch/qos/logback/core/FileAppender;->start()V

    .line 245
    .line 246
    .line 247
    new-instance v8, Lch/qos/logback/core/FileAppender;

    .line 248
    .line 249
    invoke-direct {v8}, Lch/qos/logback/core/FileAppender;-><init>()V

    .line 250
    .line 251
    .line 252
    invoke-virtual {v8, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 253
    .line 254
    .line 255
    sget-object v9, Lf/qz5;->uQ0:Ljava/lang/String;

    .line 256
    .line 257
    invoke-virtual {v8, v9}, Lch/qos/logback/core/FileAppender;->setFile(Ljava/lang/String;)V

    .line 258
    .line 259
    .line 260
    invoke-virtual {v8, v5}, Lch/qos/logback/core/FileAppender;->setAppend(Z)V

    .line 261
    .line 262
    .line 263
    invoke-virtual {v8, v4}, Lch/qos/logback/core/OutputStreamAppender;->setEncoder(Lch/qos/logback/core/encoder/Encoder;)V

    .line 264
    .line 265
    .line 266
    invoke-virtual {v8}, Lch/qos/logback/core/FileAppender;->start()V

    .line 267
    .line 268
    .line 269
    new-instance v9, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;

    .line 270
    .line 271
    invoke-direct {v9}, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;-><init>()V

    .line 272
    .line 273
    .line 274
    invoke-virtual {v9, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 275
    .line 276
    .line 277
    const-string v10, "[%thread] %msg%n"

    .line 278
    .line 279
    invoke-virtual {v9, v10}, Lch/qos/logback/core/pattern/PatternLayoutEncoderBase;->setPattern(Ljava/lang/String;)V

    .line 280
    .line 281
    .line 282
    invoke-virtual {v9}, Lch/qos/logback/classic/encoder/PatternLayoutEncoder;->start()V

    .line 283
    .line 284
    .line 285
    new-instance v10, Lch/qos/logback/classic/android/LogcatAppender;

    .line 286
    .line 287
    invoke-direct {v10}, Lch/qos/logback/classic/android/LogcatAppender;-><init>()V

    .line 288
    .line 289
    .line 290
    invoke-virtual {v10, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 291
    .line 292
    .line 293
    invoke-virtual {v10, v9}, Lch/qos/logback/classic/android/LogcatAppender;->setEncoder(Lch/qos/logback/classic/encoder/PatternLayoutEncoder;)V

    .line 294
    .line 295
    .line 296
    invoke-virtual {v10}, Lch/qos/logback/classic/android/LogcatAppender;->start()V

    .line 297
    .line 298
    .line 299
    new-instance v9, Lch/qos/logback/core/OutputStreamAppender;

    .line 300
    .line 301
    invoke-direct {v9}, Lch/qos/logback/core/OutputStreamAppender;-><init>()V

    .line 302
    .line 303
    .line 304
    invoke-virtual {v9, v0}, Lch/qos/logback/core/spi/ContextAwareBase;->setContext(Lch/qos/logback/core/Context;)V

    .line 305
    .line 306
    .line 307
    invoke-virtual {v9, v4}, Lch/qos/logback/core/OutputStreamAppender;->setEncoder(Lch/qos/logback/core/encoder/Encoder;)V

    .line 308
    .line 309
    .line 310
    new-instance v0, Lf/mu4;

    .line 311
    .line 312
    invoke-direct {v0}, Ljava/io/OutputStream;-><init>()V

    .line 313
    .line 314
    .line 315
    const/16 v4, 0x400

    .line 316
    .line 317
    new-array v11, v4, [B

    .line 318
    .line 319
    iput-object v11, v0, Lf/mu4;->T31:[B

    .line 320
    .line 321
    iput v5, v0, Lf/mu4;->s50:I

    .line 322
    .line 323
    invoke-virtual {v9, v0}, Lch/qos/logback/core/OutputStreamAppender;->setOutputStream(Ljava/io/OutputStream;)V

    .line 324
    .line 325
    .line 326
    invoke-virtual {v9}, Lch/qos/logback/core/OutputStreamAppender;->start()V

    .line 327
    .line 328
    .line 329
    const-string v0, "ROOT"

    .line 330
    .line 331
    invoke-static {v0}, Lf/tv7;->xr0(Ljava/lang/String;)Lf/xv7;

    .line 332
    .line 333
    .line 334
    move-result-object v0

    .line 335
    check-cast v0, Lch/qos/logback/classic/Logger;

    .line 336
    .line 337
    invoke-virtual {v0}, Lch/qos/logback/classic/Logger;->detachAndStopAllAppenders()V

    .line 338
    .line 339
    .line 340
    invoke-virtual {v0, v7}, Lch/qos/logback/classic/Logger;->addAppender(Lch/qos/logback/core/Appender;)V

    .line 341
    .line 342
    .line 343
    invoke-virtual {v0, v10}, Lch/qos/logback/classic/Logger;->addAppender(Lch/qos/logback/core/Appender;)V

    .line 344
    .line 345
    .line 346
    invoke-virtual {v0, v9}, Lch/qos/logback/classic/Logger;->addAppender(Lch/qos/logback/core/Appender;)V

    .line 347
    .line 348
    .line 349
    const-string v0, "mod"

    .line 350
    .line 351
    invoke-static {v0}, Lf/tv7;->xr0(Ljava/lang/String;)Lf/xv7;

    .line 352
    .line 353
    .line 354
    move-result-object v0

    .line 355
    check-cast v0, Lch/qos/logback/classic/Logger;

    .line 356
    .line 357
    invoke-virtual {v0}, Lch/qos/logback/classic/Logger;->detachAndStopAllAppenders()V

    .line 358
    .line 359
    .line 360
    invoke-virtual {v0, v5}, Lch/qos/logback/classic/Logger;->setAdditive(Z)V

    .line 361
    .line 362
    .line 363
    invoke-virtual {v0, v8}, Lch/qos/logback/classic/Logger;->addAppender(Lch/qos/logback/core/Appender;)V

    .line 364
    .line 365
    .line 366
    invoke-virtual {v0, v10}, Lch/qos/logback/classic/Logger;->addAppender(Lch/qos/logback/core/Appender;)V

    .line 367
    .line 368
    .line 369
    sget-object v0, Lf/le5;->Vl1:Ljava/lang/String;

    .line 370
    .line 371
    invoke-static {}, Ljava/util/logging/LogManager;->getLogManager()Ljava/util/logging/LogManager;

    .line 372
    .line 373
    .line 374
    move-result-object v0

    .line 375
    const-string v7, ""

    .line 376
    .line 377
    invoke-virtual {v0, v7}, Ljava/util/logging/LogManager;->getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;

    .line 378
    .line 379
    .line 380
    move-result-object v0

    .line 381
    invoke-virtual {v0}, Ljava/util/logging/Logger;->getHandlers()[Ljava/util/logging/Handler;

    .line 382
    .line 383
    .line 384
    move-result-object v7

    .line 385
    array-length v8, v7

    .line 386
    const/4 v9, 0x0

    .line 387
    :goto_182
    if-ge v9, v8, :cond_18c

    .line 388
    .line 389
    aget-object v10, v7, v9

    .line 390
    .line 391
    invoke-virtual {v0, v10}, Ljava/util/logging/Logger;->removeHandler(Ljava/util/logging/Handler;)V

    .line 392
    .line 393
    .line 394
    add-int/lit8 v9, v9, 0x1

    .line 395
    .line 396
    goto :goto_182

    .line 397
    :cond_18c
    invoke-static {}, Lf/le5;->mx0()V

    .line 398
    .line 399
    .line 400
    :try_start_18f
    const-string v0, "http.agent"

    .line 401
    .line 402
    new-instance v7, Ljava/lang/StringBuilder;

    .line 403
    .line 404
    invoke-direct {v7}, Ljava/lang/StringBuilder;-><init>()V

    .line 405
    .line 406
    .line 407
    const-string v8, "Mozilla/5.0 (PokeMMO; Client r"

    .line 408
    .line 409
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 410
    .line 411
    .line 412
    sget v8, Lf/t40;->NB1:I

    .line 413
    .line 414
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 415
    .line 416
    .line 417
    const-string v8, "; "

    .line 418
    .line 419
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 420
    .line 421
    .line 422
    sget-object v8, Lf/dy2;->RS:Lf/dy2;

    .line 423
    .line 424
    iget-object v8, v8, Lf/dy2;->Mu0:Ljava/lang/String;

    .line 425
    .line 426
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 427
    .line 428
    .line 429
    const-string v8, ")"

    .line 430
    .line 431
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 432
    .line 433
    .line 434
    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 435
    .line 436
    .line 437
    move-result-object v7

    .line 438
    invoke-static {v0, v7}, Ljava/lang/System;->setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    :try_end_1b8
    .catch Ljava/lang/Exception; {:try_start_18f .. :try_end_1b8} :catch_1b9

    .line 439
    .line 440
    .line 441
    goto :goto_1ba

    .line 442
    :catch_1b9
    nop

    .line 443
    :goto_1ba
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 444
    .line 445
    const-string v7, "Starting PokeMMO Client. Client revision: {}{}"

    .line 446
    .line 447
    sget v8, Lf/t40;->NB1:I

    .line 448
    .line 449
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 450
    .line 451
    .line 452
    move-result-object v8

    .line 453
    sget-boolean v9, Lf/t40;->jX1:Z

    .line 454
    .line 455
    if-eqz v9, :cond_1cb

    .line 456
    .line 457
    const-string v9, " (From File)"

    .line 458
    .line 459
    goto :goto_1cd

    .line 460
    :cond_1cb
    const-string v9, ""

    .line 461
    .line 462
    :goto_1cd
    invoke-interface {v0, v7, v8, v9}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 463
    .line 464
    .line 465
    const-string v7, "https://pokemmo.com"

    .line 466
    .line 467
    invoke-interface {v0, v7}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 468
    .line 469
    .line 470
    new-instance v7, Lf/b46;

    .line 471
    .line 472
    invoke-direct {v7}, Lf/b46;-><init>()V

    .line 473
    .line 474
    .line 475
    const-string v8, "mmo"

    .line 476
    .line 477
    invoke-virtual {v7, v8}, Lf/b46;->Yu0(Ljava/lang/String;)V

    .line 478
    .line 479
    .line 480
    sget-object v7, Lf/p37;->yH1:Lf/th;

    .line 481
    .line 482
    const-string v8, "java.vendor"

    .line 483
    .line 484
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 485
    .line 486
    .line 487
    move-result-object v9

    .line 488
    const/4 v10, 0x6

    .line 489
    invoke-virtual {v7, v10, v9}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 490
    .line 491
    .line 492
    const-string v9, "java.version"

    .line 493
    .line 494
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 495
    .line 496
    .line 497
    move-result-object v10

    .line 498
    const/4 v11, 0x7

    .line 499
    invoke-virtual {v7, v11, v10}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 500
    .line 501
    .line 502
    const-string v10, "java_vendor"

    .line 503
    .line 504
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 505
    .line 506
    .line 507
    move-result-object v8

    .line 508
    const-string v12, "Java Vendor"

    .line 509
    .line 510
    invoke-static {v10, v8, v12, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 511
    .line 512
    .line 513
    const-string v8, "java_version"

    .line 514
    .line 515
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 516
    .line 517
    .line 518
    move-result-object v9

    .line 519
    const-string v10, "Java Version"

    .line 520
    .line 521
    invoke-static {v8, v9, v10, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 522
    .line 523
    .line 524
    const-string v8, "java_home"

    .line 525
    .line 526
    const-string v9, "java.home"

    .line 527
    .line 528
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 529
    .line 530
    .line 531
    move-result-object v9

    .line 532
    const-string v10, "Java Home"

    .line 533
    .line 534
    invoke-static {v8, v9, v10, v3}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 535
    .line 536
    .line 537
    const-string v8, "client_home"

    .line 538
    .line 539
    const-string v9, "user.dir"

    .line 540
    .line 541
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 542
    .line 543
    .line 544
    move-result-object v9

    .line 545
    const-string v10, "Client Home"

    .line 546
    .line 547
    invoke-static {v8, v9, v10, v3}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 548
    .line 549
    .line 550
    const-string v8, "os.name"

    .line 551
    .line 552
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 553
    .line 554
    .line 555
    move-result-object v9

    .line 556
    const/4 v10, 0x3

    .line 557
    invoke-virtual {v7, v10, v9}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 558
    .line 559
    .line 560
    const-string v9, "os.version"

    .line 561
    .line 562
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 563
    .line 564
    .line 565
    move-result-object v10

    .line 566
    const/4 v12, 0x4

    .line 567
    invoke-virtual {v7, v12, v10}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 568
    .line 569
    .line 570
    const-string v10, "os.arch"

    .line 571
    .line 572
    invoke-static {v10}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 573
    .line 574
    .line 575
    move-result-object v13

    .line 576
    const/4 v14, 0x5

    .line 577
    invoke-virtual {v7, v14, v13}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 578
    .line 579
    .line 580
    const-string v13, "os_name"

    .line 581
    .line 582
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 583
    .line 584
    .line 585
    move-result-object v8

    .line 586
    const-string v15, "OS name"

    .line 587
    .line 588
    invoke-static {v13, v8, v15, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 589
    .line 590
    .line 591
    const-string v8, "os_version"

    .line 592
    .line 593
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 594
    .line 595
    .line 596
    move-result-object v9

    .line 597
    const-string v13, "OS version"

    .line 598
    .line 599
    invoke-static {v8, v9, v13, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 600
    .line 601
    .line 602
    const-string v8, "os_arch"

    .line 603
    .line 604
    invoke-static {v10}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 605
    .line 606
    .line 607
    move-result-object v9

    .line 608
    const-string v10, "OS arch"

    .line 609
    .line 610
    invoke-static {v8, v9, v10, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 611
    .line 612
    .line 613
    const-string v8, "os_abi"

    .line 614
    .line 615
    const-string v9, "sun.arch.abi"

    .line 616
    .line 617
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 618
    .line 619
    .line 620
    move-result-object v9

    .line 621
    const-string v10, "OS abi"

    .line 622
    .line 623
    invoke-static {v8, v9, v10, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 624
    .line 625
    .line 626
    const-string v8, "java.runtime.name"

    .line 627
    .line 628
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 629
    .line 630
    .line 631
    move-result-object v9

    .line 632
    const/16 v10, 0x8

    .line 633
    .line 634
    invoke-virtual {v7, v10, v9}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 635
    .line 636
    .line 637
    const-string v9, "java.vm.version"

    .line 638
    .line 639
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 640
    .line 641
    .line 642
    move-result-object v13

    .line 643
    const/16 v15, 0x9

    .line 644
    .line 645
    invoke-virtual {v7, v15, v13}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 646
    .line 647
    .line 648
    const-string v7, "jvm_arch"

    .line 649
    .line 650
    const-string v13, "sun.arch.data.model"

    .line 651
    .line 652
    invoke-static {v13}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 653
    .line 654
    .line 655
    move-result-object v13

    .line 656
    const-string v15, "JVM arch"

    .line 657
    .line 658
    invoke-static {v7, v13, v15, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 659
    .line 660
    .line 661
    const-string v7, "jvm_runtime"

    .line 662
    .line 663
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 664
    .line 665
    .line 666
    move-result-object v8

    .line 667
    const-string v13, "JVM Runtime"

    .line 668
    .line 669
    invoke-static {v7, v8, v13, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 670
    .line 671
    .line 672
    const-string v7, "jvm_version"

    .line 673
    .line 674
    invoke-static {v9}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 675
    .line 676
    .line 677
    move-result-object v8

    .line 678
    const-string v9, "JVM version"

    .line 679
    .line 680
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 681
    .line 682
    .line 683
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    .line 684
    .line 685
    .line 686
    move-result-object v7

    .line 687
    const-string v8, "heap_memory_max"

    .line 688
    .line 689
    new-instance v9, Ljava/lang/StringBuilder;

    .line 690
    .line 691
    invoke-direct {v9}, Ljava/lang/StringBuilder;-><init>()V

    .line 692
    .line 693
    .line 694
    invoke-virtual {v7}, Ljava/lang/Runtime;->maxMemory()J

    .line 695
    .line 696
    .line 697
    move-result-wide v15

    .line 698
    const-wide/32 v17, 0x100000

    .line 699
    .line 700
    .line 701
    const/16 p1, 0x4

    .line 702
    .line 703
    div-long v12, v15, v17

    .line 704
    .line 705
    invoke-virtual {v9, v12, v13}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    .line 706
    .line 707
    .line 708
    const-string v7, " MB"

    .line 709
    .line 710
    invoke-virtual {v9, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 711
    .line 712
    .line 713
    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 714
    .line 715
    .line 716
    move-result-object v7

    .line 717
    const-string v9, "Max Heap Memory"

    .line 718
    .line 719
    invoke-static {v8, v7, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 720
    .line 721
    .line 722
    const-string v7, "locale"

    .line 723
    .line 724
    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;

    .line 725
    .line 726
    .line 727
    move-result-object v8

    .line 728
    invoke-virtual {v8}, Ljava/util/Locale;->toString()Ljava/lang/String;

    .line 729
    .line 730
    .line 731
    move-result-object v8

    .line 732
    const-string v9, "Locale"

    .line 733
    .line 734
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 735
    .line 736
    .line 737
    const-string v7, "file_encoding"

    .line 738
    .line 739
    const-string v8, "file.encoding"

    .line 740
    .line 741
    invoke-static {v8}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 742
    .line 743
    .line 744
    move-result-object v8

    .line 745
    const-string v9, "File Encoding"

    .line 746
    .line 747
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 748
    .line 749
    .line 750
    const-string v7, "libgdx_version"

    .line 751
    .line 752
    sget v8, Lf/ih7;->Pt0:I

    .line 753
    .line 754
    const-string v8, "1.14.2"

    .line 755
    .line 756
    const-string v9, "Gdx version"

    .line 757
    .line 758
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 759
    .line 760
    .line 761
    invoke-static {}, Landroid/os/Process;->myPid()I

    .line 762
    .line 763
    .line 764
    new-instance v7, Lf/zy3;

    .line 765
    .line 766
    invoke-direct {v7, v11, v5}, Lf/zy3;-><init>(IZ)V

    .line 767
    .line 768
    .line 769
    iput-object v1, v7, Lf/zy3;->TY0:Ljava/lang/Object;

    .line 770
    .line 771
    sput-object v7, Lf/p37;->IT:Lf/zy3;

    .line 772
    .line 773
    new-instance v7, Lf/k81;

    .line 774
    .line 775
    invoke-direct {v7}, Ljava/lang/Object;-><init>()V

    .line 776
    .line 777
    .line 778
    invoke-static {v7}, Ljava/lang/Thread;->setDefaultUncaughtExceptionHandler(Ljava/lang/Thread$UncaughtExceptionHandler;)V

    .line 779
    .line 780
    .line 781
    sget v7, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 782
    .line 783
    sput v7, Lf/p37;->tW1:I

    .line 784
    .line 785
    const-string v8, "android_sdk"

    .line 786
    .line 787
    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 788
    .line 789
    .line 790
    move-result-object v7

    .line 791
    const-string v9, "Android API Level"

    .line 792
    .line 793
    invoke-static {v8, v7, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 794
    .line 795
    .line 796
    const-string v7, "android_manufacturer"

    .line 797
    .line 798
    sget-object v8, Landroid/os/Build;->MANUFACTURER:Ljava/lang/String;

    .line 799
    .line 800
    const-string v9, "Android Manufacturer"

    .line 801
    .line 802
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 803
    .line 804
    .line 805
    const-string v7, "android_device"

    .line 806
    .line 807
    sget-object v8, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 808
    .line 809
    const-string v9, "Android Device"

    .line 810
    .line 811
    invoke-static {v7, v8, v9, v5}, Lf/qz5;->L30(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;Z)V

    .line 812
    .line 813
    .line 814
    const-string v7, "Software Keyboard {}"

    .line 815
    .line 816
    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    .line 817
    .line 818
    .line 819
    move-result-object v8

    .line 820
    const-string v9, "default_input_method"

    .line 821
    .line 822
    invoke-static {v8, v9}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    .line 823
    .line 824
    .line 825
    move-result-object v8

    .line 826
    invoke-interface {v0, v7, v8}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 827
    .line 828
    .line 829
    const-string v7, "Hardware Keyboard Type: {}"

    .line 830
    .line 831
    invoke-virtual {v1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    .line 832
    .line 833
    .line 834
    move-result-object v8

    .line 835
    invoke-virtual {v8}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;

    .line 836
    .line 837
    .line 838
    move-result-object v8

    .line 839
    iget v8, v8, Landroid/content/res/Configuration;->keyboard:I

    .line 840
    .line 841
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 842
    .line 843
    .line 844
    move-result-object v8

    .line 845
    invoke-interface {v0, v7, v8}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 846
    .line 847
    .line 848
    const-string v0, "audio"

    .line 849
    .line 850
    invoke-virtual {v1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    .line 851
    .line 852
    .line 853
    move-result-object v0

    .line 854
    check-cast v0, Landroid/media/AudioManager;

    .line 855
    .line 856
    :try_start_357
    const-string v7, "android.media.property.OUTPUT_FRAMES_PER_BUFFER"

    .line 857
    .line 858
    invoke-virtual {v0, v7}, Landroid/media/AudioManager;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 859
    .line 860
    .line 861
    move-result-object v7

    .line 862
    invoke-static {v7}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 863
    .line 864
    .line 865
    move-result v7

    .line 866
    mul-int/lit8 v7, v7, 0x4

    .line 867
    .line 868
    sput v7, Lf/an;->kn1:I
    :try_end_365
    .catch Ljava/lang/NumberFormatException; {:try_start_357 .. :try_end_365} :catch_365

    .line 869
    .line 870
    :catch_365
    sget-object v7, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 871
    .line 872
    const-string v8, "PROPERTY_OUTPUT_SAMPLE_RATE: {}"

    .line 873
    .line 874
    const-string v9, "android.media.property.OUTPUT_SAMPLE_RATE"

    .line 875
    .line 876
    invoke-virtual {v0, v9}, Landroid/media/AudioManager;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 877
    .line 878
    .line 879
    move-result-object v9

    .line 880
    invoke-interface {v7, v8, v9}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 881
    .line 882
    .line 883
    const-string v8, "PROPERTY_OUTPUT_FRAMES_PER_BUFFER: {}"

    .line 884
    .line 885
    const-string v9, "android.media.property.OUTPUT_FRAMES_PER_BUFFER"

    .line 886
    .line 887
    invoke-virtual {v0, v9}, Landroid/media/AudioManager;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    .line 888
    .line 889
    .line 890
    move-result-object v0

    .line 891
    invoke-interface {v7, v8, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 892
    .line 893
    .line 894
    const/16 v0, 0xa

    .line 895
    .line 896
    new-array v7, v0, [I

    .line 897
    .line 898
    fill-array-data v7, :array_71c

    .line 899
    .line 900
    .line 901
    const/4 v8, 0x0

    .line 902
    :goto_385
    const/4 v9, 0x2

    .line 903
    const/4 v11, -0x1

    .line 904
    if-ge v8, v0, :cond_3b0

    .line 905
    .line 906
    aget v12, v7, v8

    .line 907
    .line 908
    const/16 v13, 0xc

    .line 909
    .line 910
    invoke-static {v12, v13, v9}, Landroid/media/AudioTrack;->getMinBufferSize(III)I

    .line 911
    .line 912
    .line 913
    move-result v9

    .line 914
    if-eq v9, v11, :cond_39a

    .line 915
    .line 916
    const/4 v11, -0x2

    .line 917
    if-eq v9, v11, :cond_39a

    .line 918
    .line 919
    if-lez v9, :cond_39a

    .line 920
    .line 921
    const/4 v9, 0x1

    .line 922
    goto :goto_39b

    .line 923
    :cond_39a
    const/4 v9, 0x0

    .line 924
    :goto_39b
    sget-object v11, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 925
    .line 926
    const-string v13, "AudioTrack Sample Rate[{}] {}"

    .line 927
    .line 928
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 929
    .line 930
    .line 931
    move-result-object v12

    .line 932
    if-eqz v9, :cond_3a8

    .line 933
    .line 934
    const-string v9, "Supported"

    .line 935
    .line 936
    goto :goto_3aa

    .line 937
    :cond_3a8
    const-string v9, "NOT SUPPORTED"

    .line 938
    .line 939
    :goto_3aa
    invoke-interface {v11, v13, v12, v9}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 940
    .line 941
    .line 942
    add-int/lit8 v8, v8, 0x1

    .line 943
    .line 944
    goto :goto_385

    .line 945
    :cond_3b0
    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    .line 946
    .line 947
    .line 948
    new-instance v0, Ljava/io/File;

    .line 949
    .line 950
    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    .line 951
    .line 952
    .line 953
    move-result-object v7

    .line 954
    const-string v8, "config"

    .line 955
    .line 956
    invoke-direct {v0, v7, v8}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 957
    .line 958
    .line 959
    sput-object v0, Lf/ut2;->u:Ljava/io/File;

    .line 960
    .line 961
    new-instance v7, Lf/rh6;

    .line 962
    .line 963
    const-class v0, Ljava/util/Properties;

    .line 964
    .line 965
    invoke-direct {v7, v0}, Lf/rh6;-><init>(Ljava/lang/Class;)V

    .line 966
    .line 967
    .line 968
    :try_start_3c7
    invoke-virtual {v1}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    .line 969
    .line 970
    .line 971
    move-result-object v0

    .line 972
    const-string v8, "config"

    .line 973
    .line 974
    invoke-virtual {v0, v8}, Landroid/content/res/AssetManager;->list(Ljava/lang/String;)[Ljava/lang/String;

    .line 975
    .line 976
    .line 977
    move-result-object v0

    .line 978
    const/4 v8, 0x0

    .line 979
    :goto_3d2
    array-length v12, v0

    .line 980
    if-ge v8, v12, :cond_403

    .line 981
    .line 982
    new-instance v12, Ljava/util/Properties;

    .line 983
    .line 984
    invoke-direct {v12}, Ljava/util/Properties;-><init>()V

    .line 985
    .line 986
    .line 987
    invoke-virtual {v1}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    .line 988
    .line 989
    .line 990
    move-result-object v13

    .line 991
    new-instance v15, Ljava/lang/StringBuilder;

    .line 992
    .line 993
    invoke-direct {v15}, Ljava/lang/StringBuilder;-><init>()V

    .line 994
    .line 995
    .line 996
    const-string v11, "config/"

    .line 997
    .line 998
    invoke-virtual {v15, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 999
    .line 1000
    .line 1001
    aget-object v11, v0, v8

    .line 1002
    .line 1003
    invoke-virtual {v15, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 1004
    .line 1005
    .line 1006
    invoke-virtual {v15}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1007
    .line 1008
    .line 1009
    move-result-object v11

    .line 1010
    invoke-virtual {v13, v11}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    .line 1011
    .line 1012
    .line 1013
    move-result-object v11

    .line 1014
    invoke-virtual {v12, v11}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 1015
    .line 1016
    .line 1017
    invoke-virtual {v7, v12}, Lf/rh6;->AU1(Ljava/lang/Object;)V
    :try_end_3fb
    .catch Ljava/lang/Exception; {:try_start_3c7 .. :try_end_3fb} :catch_3ff

    .line 1018
    .line 1019
    .line 1020
    add-int/lit8 v8, v8, 0x1

    .line 1021
    .line 1022
    const/4 v11, -0x1

    .line 1023
    goto :goto_3d2

    .line 1024
    :catch_3ff
    move-exception v0

    .line 1025
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    .line 1026
    .line 1027
    .line 1028
    :cond_403
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 1029
    .line 1030
    .line 1031
    move-result-object v0

    .line 1032
    const/16 v8, 0x80

    .line 1033
    .line 1034
    invoke-virtual {v0, v8}, Landroid/view/Window;->addFlags(I)V

    .line 1035
    .line 1036
    .line 1037
    iget-object v0, v7, Lf/rh6;->iu1:[Ljava/lang/Object;

    .line 1038
    .line 1039
    iget v7, v7, Lf/rh6;->Mm:I

    .line 1040
    .line 1041
    invoke-static {v0, v7}, Ljava/util/Arrays;->copyOf([Ljava/lang/Object;I)[Ljava/lang/Object;

    .line 1042
    .line 1043
    .line 1044
    move-result-object v0

    .line 1045
    check-cast v0, [Ljava/util/Properties;

    .line 1046
    .line 1047
    invoke-static {v0}, Lf/ut2;->G00([Ljava/util/Properties;)V

    .line 1048
    .line 1049
    .line 1050
    invoke-virtual {v1}, Landroid/content/Context;->getCacheDir()Ljava/io/File;

    .line 1051
    .line 1052
    .line 1053
    move-result-object v0

    .line 1054
    sput-object v0, Lf/g53;->FK:Ljava/io/File;

    .line 1055
    .line 1056
    new-instance v0, Lf/i72;

    .line 1057
    .line 1058
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 1059
    .line 1060
    .line 1061
    sput-object v0, Lf/p37;->Vh0:Lf/i72;

    .line 1062
    .line 1063
    new-instance v0, Lf/nq;

    .line 1064
    .line 1065
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 1066
    .line 1067
    .line 1068
    iput-object v6, v0, Lf/nq;->dO0:Lf/z46;

    .line 1069
    .line 1070
    iput-object v1, v0, Lf/nq;->Mx0:Leu/pokemmo/client/AndroidLauncher;

    .line 1071
    .line 1072
    sput-object v0, Lf/p37;->LpT1:Lf/nq;

    .line 1073
    .line 1074
    new-instance v0, Lf/o44;

    .line 1075
    .line 1076
    const-string v7, "Error"

    .line 1077
    .line 1078
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 1079
    .line 1080
    .line 1081
    iput-boolean v5, v0, Lf/o44;->dD:Z

    .line 1082
    .line 1083
    new-instance v8, Lf/a97;

    .line 1084
    .line 1085
    const/16 v11, 0x7530

    .line 1086
    .line 1087
    invoke-direct {v8, v11}, Lf/a97;-><init>(I)V

    .line 1088
    .line 1089
    .line 1090
    iput-object v8, v0, Lf/o44;->lt:Lf/a97;

    .line 1091
    .line 1092
    iput-object v6, v0, Lf/o44;->dK:Lf/fq6;

    .line 1093
    .line 1094
    iput-object v1, v0, Lf/o44;->o31:Leu/pokemmo/client/AndroidLauncher;

    .line 1095
    .line 1096
    sget-object v6, Lf/p37;->yH1:Lf/th;

    .line 1097
    .line 1098
    const/16 v8, 0x10

    .line 1099
    .line 1100
    sget-object v11, Landroid/os/Build;->BOARD:Ljava/lang/String;

    .line 1101
    .line 1102
    invoke-virtual {v6, v8, v11}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1103
    .line 1104
    .line 1105
    sget-object v8, Landroid/os/Build;->BRAND:Ljava/lang/String;

    .line 1106
    .line 1107
    const/16 v11, 0x11

    .line 1108
    .line 1109
    invoke-virtual {v6, v11, v8}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1110
    .line 1111
    .line 1112
    const/16 v8, 0x12

    .line 1113
    .line 1114
    sget-object v12, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 1115
    .line 1116
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1117
    .line 1118
    .line 1119
    const/16 v8, 0x13

    .line 1120
    .line 1121
    sget-object v12, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 1122
    .line 1123
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1124
    .line 1125
    .line 1126
    const/16 v8, 0x14

    .line 1127
    .line 1128
    sget-object v12, Landroid/os/Build;->HARDWARE:Ljava/lang/String;

    .line 1129
    .line 1130
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1131
    .line 1132
    .line 1133
    const/16 v8, 0x15

    .line 1134
    .line 1135
    sget-object v12, Landroid/os/Build;->MANUFACTURER:Ljava/lang/String;

    .line 1136
    .line 1137
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1138
    .line 1139
    .line 1140
    const/16 v8, 0x16

    .line 1141
    .line 1142
    sget-object v12, Landroid/os/Build;->MODEL:Ljava/lang/String;

    .line 1143
    .line 1144
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1145
    .line 1146
    .line 1147
    const/16 v8, 0x17

    .line 1148
    .line 1149
    sget-object v12, Landroid/os/Build;->PRODUCT:Ljava/lang/String;

    .line 1150
    .line 1151
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1152
    .line 1153
    .line 1154
    const/16 v8, 0x18

    .line 1155
    .line 1156
    invoke-static {}, Landroid/os/Build;->getRadioVersion()Ljava/lang/String;

    .line 1157
    .line 1158
    .line 1159
    move-result-object v12

    .line 1160
    invoke-virtual {v6, v8, v12}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1161
    .line 1162
    .line 1163
    new-instance v8, Ljava/lang/StringBuilder;

    .line 1164
    .line 1165
    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    .line 1166
    .line 1167
    .line 1168
    sget v12, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 1169
    .line 1170
    invoke-virtual {v8, v12}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 1171
    .line 1172
    .line 1173
    const-string v12, ""

    .line 1174
    .line 1175
    invoke-virtual {v8, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 1176
    .line 1177
    .line 1178
    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1179
    .line 1180
    .line 1181
    move-result-object v8

    .line 1182
    invoke-virtual {v6, v2, v8}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1183
    .line 1184
    .line 1185
    new-instance v2, Ljava/lang/StringBuilder;

    .line 1186
    .line 1187
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 1188
    .line 1189
    .line 1190
    sget-object v6, Landroid/os/Build;->SUPPORTED_ABIS:[Ljava/lang/String;

    .line 1191
    .line 1192
    array-length v8, v6

    .line 1193
    const/4 v13, 0x0

    .line 1194
    :goto_4a9
    if-ge v13, v8, :cond_4c0

    .line 1195
    .line 1196
    aget-object v15, v6, v13

    .line 1197
    .line 1198
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->length()I

    .line 1199
    .line 1200
    .line 1201
    move-result v17

    .line 1202
    if-lez v17, :cond_4b8

    .line 1203
    .line 1204
    const/16 v11, 0x2c

    .line 1205
    .line 1206
    invoke-virtual {v2, v11}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    .line 1207
    .line 1208
    .line 1209
    :cond_4b8
    invoke-virtual {v2, v15}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 1210
    .line 1211
    .line 1212
    add-int/lit8 v13, v13, 0x1

    .line 1213
    .line 1214
    const/16 v11, 0x11

    .line 1215
    .line 1216
    goto :goto_4a9

    .line 1217
    :cond_4c0
    sget-object v6, Lf/p37;->yH1:Lf/th;

    .line 1218
    .line 1219
    const/16 v8, 0x1d

    .line 1220
    .line 1221
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1222
    .line 1223
    .line 1224
    move-result-object v2

    .line 1225
    invoke-virtual {v6, v8, v2}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1226
    .line 1227
    .line 1228
    new-instance v2, Ljava/lang/StringBuilder;

    .line 1229
    .line 1230
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 1231
    .line 1232
    .line 1233
    sget-wide v9, Landroid/os/Build;->TIME:J

    .line 1234
    .line 1235
    invoke-static {v2, v9, v10, v12}, Lf/cz7;->Mq0(Ljava/lang/StringBuilder;JLjava/lang/String;)Ljava/lang/String;

    .line 1236
    .line 1237
    .line 1238
    move-result-object v2

    .line 1239
    const/16 v9, 0x1e

    .line 1240
    .line 1241
    invoke-virtual {v6, v9, v2}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1242
    .line 1243
    .line 1244
    const/16 v2, 0x19

    .line 1245
    .line 1246
    const/16 v10, 0x1a

    .line 1247
    .line 1248
    :try_start_4df
    const-string v12, "phone"

    .line 1249
    .line 1250
    invoke-virtual {v1, v12}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    .line 1251
    .line 1252
    .line 1253
    move-result-object v12

    .line 1254
    check-cast v12, Landroid/telephony/TelephonyManager;

    .line 1255
    .line 1256
    sget v13, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 1257
    .line 1258
    if-lt v13, v10, :cond_4fe

    .line 1259
    .line 1260
    invoke-virtual {v12, v5}, Landroid/telephony/TelephonyManager;->getSimState(I)I

    .line 1261
    .line 1262
    .line 1263
    move-result v13

    .line 1264
    if-eq v13, v14, :cond_4fa

    .line 1265
    .line 1266
    invoke-virtual {v12, v3}, Landroid/telephony/TelephonyManager;->getSimState(I)I

    .line 1267
    .line 1268
    .line 1269
    move-result v12

    .line 1270
    if-ne v12, v14, :cond_4f8

    .line 1271
    .line 1272
    goto :goto_4fa

    .line 1273
    :cond_4f8
    const/4 v12, 0x0

    .line 1274
    goto :goto_4fb

    .line 1275
    :cond_4fa
    :goto_4fa
    const/4 v12, 0x1

    .line 1276
    :goto_4fb
    iput-boolean v12, v0, Lf/o44;->dD:Z

    .line 1277
    .line 1278
    goto :goto_509

    .line 1279
    :cond_4fe
    invoke-virtual {v12}, Landroid/telephony/TelephonyManager;->getSimState()I

    .line 1280
    .line 1281
    .line 1282
    move-result v12

    .line 1283
    if-ne v12, v14, :cond_506

    .line 1284
    .line 1285
    const/4 v12, 0x1

    .line 1286
    goto :goto_507

    .line 1287
    :cond_506
    const/4 v12, 0x0

    .line 1288
    :goto_507
    iput-boolean v12, v0, Lf/o44;->dD:Z

    .line 1289
    .line 1290
    :goto_509
    iget-boolean v12, v0, Lf/o44;->dD:Z

    .line 1291
    .line 1292
    invoke-static {v12}, Ljava/lang/Boolean;->toString(Z)Ljava/lang/String;

    .line 1293
    .line 1294
    .line 1295
    move-result-object v12

    .line 1296
    invoke-virtual {v6, v2, v12}, Lf/th;->yU0(BLjava/lang/String;)V
    :try_end_512
    .catch Ljava/lang/Exception; {:try_start_4df .. :try_end_512} :catch_513

    .line 1297
    .line 1298
    .line 1299
    goto :goto_518

    .line 1300
    :catch_513
    sget-object v6, Lf/p37;->yH1:Lf/th;

    .line 1301
    .line 1302
    invoke-virtual {v6, v2, v7}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1303
    .line 1304
    .line 1305
    :goto_518
    :try_start_518
    invoke-static {}, Lf/o44;->y21()Z

    .line 1306
    .line 1307
    .line 1308
    move-result v2

    .line 1309
    sget-object v6, Lf/p37;->yH1:Lf/th;

    .line 1310
    .line 1311
    invoke-static {v2}, Ljava/lang/Boolean;->toString(Z)Ljava/lang/String;

    .line 1312
    .line 1313
    .line 1314
    move-result-object v2

    .line 1315
    invoke-virtual {v6, v10, v2}, Lf/th;->yU0(BLjava/lang/String;)V
    :try_end_525
    .catch Ljava/lang/Exception; {:try_start_518 .. :try_end_525} :catch_526

    .line 1316
    .line 1317
    .line 1318
    goto :goto_52b

    .line 1319
    :catch_526
    sget-object v2, Lf/p37;->yH1:Lf/th;

    .line 1320
    .line 1321
    invoke-virtual {v2, v10, v7}, Lf/th;->yU0(BLjava/lang/String;)V

    .line 1322
    .line 1323
    .line 1324
    :goto_52b
    :try_start_52b
    invoke-virtual {v1}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    .line 1325
    .line 1326
    .line 1327
    move-result-object v2

    .line 1328
    invoke-virtual {v1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    .line 1329
    .line 1330
    .line 1331
    move-result-object v6

    .line 1332
    invoke-virtual {v2, v6}, Landroid/content/pm/PackageManager;->getInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;

    .line 1333
    .line 1334
    .line 1335
    move-result-object v2

    .line 1336
    sget-object v6, Lf/p37;->yH1:Lf/th;

    .line 1337
    .line 1338
    const/16 v7, 0x1b

    .line 1339
    .line 1340
    invoke-virtual {v6, v7, v2}, Lf/th;->yU0(BLjava/lang/String;)V
    :try_end_53e
    .catch Ljava/lang/Exception; {:try_start_52b .. :try_end_53e} :catch_53e

    .line 1341
    .line 1342
    .line 1343
    :catch_53e
    sget-object v2, Lf/p37;->yH1:Lf/th;

    .line 1344
    .line 1345
    invoke-virtual {v1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    .line 1346
    .line 1347
    .line 1348
    move-result-object v6

    .line 1349
    invoke-virtual {v0, v6}, Lf/o44;->Xx1(Ljava/lang/String;)[B

    .line 1350
    .line 1351
    .line 1352
    move-result-object v6

    .line 1353
    iput-object v6, v2, Lf/th;->vr1:Ljava/lang/Object;

    .line 1354
    .line 1355
    new-instance v2, Landroid/os/Handler;

    .line 1356
    .line 1357
    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    .line 1358
    .line 1359
    .line 1360
    move-result-object v6

    .line 1361
    invoke-direct {v2, v6}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    .line 1362
    .line 1363
    .line 1364
    iput-object v2, v0, Lf/o44;->o02:Landroid/os/Handler;

    .line 1365
    .line 1366
    sput-object v0, Lf/p37;->Uh0:Lf/o44;

    .line 1367
    .line 1368
    new-instance v0, Lf/t72;

    .line 1369
    .line 1370
    invoke-direct {v0}, Lf/aw3;-><init>()V

    .line 1371
    .line 1372
    .line 1373
    sput-object v0, Lf/p37;->X20:Lf/t72;

    .line 1374
    .line 1375
    new-instance v0, Lf/cp7;

    .line 1376
    .line 1377
    invoke-direct {v0}, Lf/cp7;-><init>()V

    .line 1378
    .line 1379
    .line 1380
    iput-object v0, v1, Leu/pokemmo/client/AndroidLauncher;->dP:Lf/cp7;

    .line 1381
    .line 1382
    sget v2, Lf/ms5;->vt0:I

    .line 1383
    .line 1384
    const/16 v8, 0x8

    .line 1385
    .line 1386
    invoke-static {v2, v8}, Ljava/lang/Math;->min(II)I

    .line 1387
    .line 1388
    .line 1389
    move-result v2

    .line 1390
    iput v2, v0, Lf/cp7;->nc1:I

    .line 1391
    .line 1392
    iget-object v0, v1, Leu/pokemmo/client/AndroidLauncher;->dP:Lf/cp7;

    .line 1393
    .line 1394
    sget-boolean v2, Lf/ms5;->O50:Z

    .line 1395
    .line 1396
    iput-boolean v2, v0, Lf/cp7;->vx0:Z

    .line 1397
    .line 1398
    iput-boolean v5, v0, Lf/cp7;->Gg:Z

    .line 1399
    .line 1400
    iput-boolean v5, v0, Lf/cp7;->UD:Z

    .line 1401
    .line 1402
    new-instance v0, Lf/q97;

    .line 1403
    .line 1404
    invoke-direct {v0}, Lf/nq7;-><init>()V

    .line 1405
    .line 1406
    .line 1407
    iget-object v2, v1, Leu/pokemmo/client/AndroidLauncher;->dP:Lf/cp7;

    .line 1408
    .line 1409
    iget-object v6, v2, Lf/cp7;->eb1:Lf/jo2;

    .line 1410
    .line 1411
    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 1412
    .line 1413
    .line 1414
    const-class v6, Lf/a05;

    .line 1415
    .line 1416
    monitor-enter v6

    .line 1417
    :try_start_588
    sget-boolean v7, Lf/a05;->zT1:Z
    :try_end_58a
    .catchall {:try_start_588 .. :try_end_58a} :catchall_718

    .line 1418
    .line 1419
    if-eqz v7, :cond_58e

    .line 1420
    .line 1421
    monitor-exit v6

    .line 1422
    goto :goto_59b

    .line 1423
    :cond_58e
    :try_start_58e
    new-instance v7, Lf/b46;

    .line 1424
    .line 1425
    invoke-direct {v7}, Lf/b46;-><init>()V

    .line 1426
    .line 1427
    .line 1428
    const-string v8, "gdx"

    .line 1429
    .line 1430
    invoke-virtual {v7, v8}, Lf/b46;->Yu0(Ljava/lang/String;)V

    .line 1431
    .line 1432
    .line 1433
    sput-boolean v3, Lf/a05;->zT1:Z
    :try_end_59a
    .catchall {:try_start_58e .. :try_end_59a} :catchall_718

    .line 1434
    .line 1435
    monitor-exit v6

    .line 1436
    :goto_59b
    new-instance v6, Lf/ma3;

    .line 1437
    .line 1438
    invoke-direct {v6}, Lf/ma3;-><init>()V

    .line 1439
    .line 1440
    .line 1441
    iput-object v6, v1, Lf/nx3;->Rt1:Lf/ma3;

    .line 1442
    .line 1443
    new-instance v6, Lf/bv3;

    .line 1444
    .line 1445
    iget-object v7, v2, Lf/cp7;->vT1:Lf/o63;

    .line 1446
    .line 1447
    if-nez v7, :cond_5ad

    .line 1448
    .line 1449
    new-instance v7, Lf/o63;

    .line 1450
    .line 1451
    invoke-direct {v7}, Lf/o63;-><init>()V

    .line 1452
    .line 1453
    .line 1454
    :cond_5ad
    invoke-direct {v6, v1, v2, v7}, Lf/bv3;-><init>(Lf/ss8;Lf/cp7;Lf/uk7;)V

    .line 1455
    .line 1456
    .line 1457
    iput-object v6, v1, Lf/nx3;->ed1:Lf/bv3;

    .line 1458
    .line 1459
    iget-object v6, v6, Lf/bv3;->BX0:Lf/y77;

    .line 1460
    .line 1461
    new-instance v7, Lf/qa7;

    .line 1462
    .line 1463
    invoke-direct {v7, v1, v1, v6, v2}, Lf/qa7;-><init>(Lf/x97;Landroid/content/Context;Landroid/view/View;Lf/cp7;)V

    .line 1464
    .line 1465
    .line 1466
    iput-object v7, v1, Lf/nx3;->x81:Lf/qa7;

    .line 1467
    .line 1468
    new-instance v6, Lf/ez;

    .line 1469
    .line 1470
    invoke-direct {v6, v1, v2}, Lf/ez;-><init>(Landroid/content/Context;Lf/cp7;)V

    .line 1471
    .line 1472
    .line 1473
    iput-object v6, v1, Lf/nx3;->iM:Lf/ez;

    .line 1474
    .line 1475
    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    .line 1476
    .line 1477
    .line 1478
    new-instance v6, Lf/u43;

    .line 1479
    .line 1480
    invoke-virtual {v1}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    .line 1481
    .line 1482
    .line 1483
    move-result-object v7

    .line 1484
    invoke-direct {v6, v7, v1, v3}, Lf/u43;-><init>(Landroid/content/res/AssetManager;Landroid/content/ContextWrapper;Z)V

    .line 1485
    .line 1486
    .line 1487
    iput-object v6, v1, Lf/nx3;->w60:Lf/u43;

    .line 1488
    .line 1489
    new-instance v6, Lf/nc2;

    .line 1490
    .line 1491
    invoke-direct {v6, v1, v2}, Lf/nc2;-><init>(Lf/ss8;Lf/cp7;)V

    .line 1492
    .line 1493
    .line 1494
    iput-object v6, v1, Lf/nx3;->zM:Lf/nc2;

    .line 1495
    .line 1496
    iput-object v0, v1, Lf/nx3;->UN0:Lf/q97;

    .line 1497
    .line 1498
    new-instance v0, Landroid/os/Handler;

    .line 1499
    .line 1500
    invoke-direct {v0}, Landroid/os/Handler;-><init>()V

    .line 1501
    .line 1502
    .line 1503
    iput-object v0, v1, Lf/nx3;->PB0:Landroid/os/Handler;

    .line 1504
    .line 1505
    iget-boolean v0, v2, Lf/cp7;->vx0:Z

    .line 1506
    .line 1507
    iput-boolean v0, v1, Lf/nx3;->nE0:Z

    .line 1508
    .line 1509
    new-instance v0, Lf/z16;

    .line 1510
    .line 1511
    invoke-direct {v0, v1}, Lf/z16;-><init>(Landroid/content/Context;)V

    .line 1512
    .line 1513
    .line 1514
    iput-object v0, v1, Lf/nx3;->zC0:Lf/z16;

    .line 1515
    .line 1516
    new-instance v0, Lf/sd0;

    .line 1517
    .line 1518
    invoke-direct {v0, v1, v5}, Lf/sd0;-><init>(Lf/x97;I)V

    .line 1519
    .line 1520
    .line 1521
    invoke-virtual {v1, v0}, Lf/nx3;->static(Lf/ll4;)V

    .line 1522
    .line 1523
    .line 1524
    sput-object v1, Lf/dq7;->Et:Lf/nx3;

    .line 1525
    .line 1526
    iget-object v0, v1, Lf/nx3;->x81:Lf/qa7;

    .line 1527
    .line 1528
    sput-object v0, Lf/dq7;->Nc1:Lf/qa7;

    .line 1529
    .line 1530
    iget-object v0, v1, Lf/nx3;->iM:Lf/ez;

    .line 1531
    .line 1532
    sput-object v0, Lf/dq7;->Gu1:Lf/ez;

    .line 1533
    .line 1534
    iget-object v0, v1, Lf/nx3;->w60:Lf/u43;

    .line 1535
    .line 1536
    sput-object v0, Lf/dq7;->vZ1:Lf/u43;

    .line 1537
    .line 1538
    iget-object v0, v1, Lf/nx3;->ed1:Lf/bv3;

    .line 1539
    .line 1540
    sput-object v0, Lf/dq7;->NU0:Lf/bv3;

    .line 1541
    .line 1542
    iget-object v0, v1, Lf/nx3;->zM:Lf/nc2;

    .line 1543
    .line 1544
    sput-object v0, Lf/dq7;->yT:Lf/nc2;

    .line 1545
    .line 1546
    :try_start_609
    invoke-virtual {v1, v3}, Landroid/app/Activity;->requestWindowFeature(I)Z
    :try_end_60c
    .catch Ljava/lang/Exception; {:try_start_609 .. :try_end_60c} :catch_60d

    .line 1547
    .line 1548
    .line 1549
    goto :goto_61f

    .line 1550
    :catch_60d
    move-exception v0

    .line 1551
    const-string v2, "Content already displayed, cannot request FEATURE_NO_TITLE"

    .line 1552
    .line 1553
    const-string v6, "AndroidApplication"

    .line 1554
    .line 1555
    iget v7, v1, Lf/nx3;->DX1:I

    .line 1556
    .line 1557
    const/4 v11, 0x2

    .line 1558
    if-lt v7, v11, :cond_61f

    .line 1559
    .line 1560
    iget-object v7, v1, Lf/nx3;->Rt1:Lf/ma3;

    .line 1561
    .line 1562
    invoke-virtual {v7}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 1563
    .line 1564
    .line 1565
    invoke-static {v6, v2, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 1566
    .line 1567
    .line 1568
    :cond_61f
    :goto_61f
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 1569
    .line 1570
    .line 1571
    move-result-object v0

    .line 1572
    invoke-virtual {v0, v4, v4}, Landroid/view/Window;->setFlags(II)V

    .line 1573
    .line 1574
    .line 1575
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 1576
    .line 1577
    .line 1578
    move-result-object v0

    .line 1579
    const/16 v2, 0x800

    .line 1580
    .line 1581
    invoke-virtual {v0, v2}, Landroid/view/Window;->clearFlags(I)V

    .line 1582
    .line 1583
    .line 1584
    iget-object v0, v1, Lf/nx3;->ed1:Lf/bv3;

    .line 1585
    .line 1586
    iget-object v0, v0, Lf/bv3;->BX0:Lf/y77;

    .line 1587
    .line 1588
    new-instance v2, Landroid/widget/FrameLayout$LayoutParams;

    .line 1589
    .line 1590
    const/4 v4, -0x1

    .line 1591
    invoke-direct {v2, v4, v4}, Landroid/widget/FrameLayout$LayoutParams;-><init>(II)V

    .line 1592
    .line 1593
    .line 1594
    const/16 v4, 0x11

    .line 1595
    .line 1596
    iput v4, v2, Landroid/widget/FrameLayout$LayoutParams;->gravity:I

    .line 1597
    .line 1598
    invoke-virtual {v1, v0, v2}, Landroid/app/Activity;->setContentView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 1599
    .line 1600
    .line 1601
    iget-boolean v0, v1, Lf/nx3;->nE0:Z

    .line 1602
    .line 1603
    invoke-virtual {v1, v0}, Lf/nx3;->lE0(Z)V

    .line 1604
    .line 1605
    .line 1606
    iget-boolean v0, v1, Lf/nx3;->nE0:Z

    .line 1607
    .line 1608
    if-eqz v0, :cond_671

    .line 1609
    .line 1610
    new-instance v0, Lf/ei1;

    .line 1611
    .line 1612
    invoke-direct {v0}, Lf/ei1;-><init>()V

    .line 1613
    .line 1614
    .line 1615
    :try_start_64e
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 1616
    .line 1617
    .line 1618
    move-result-object v0

    .line 1619
    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    .line 1620
    .line 1621
    .line 1622
    move-result-object v0

    .line 1623
    new-instance v2, Lf/ec9;

    .line 1624
    .line 1625
    invoke-direct {v2, v1}, Lf/ec9;-><init>(Leu/pokemmo/client/AndroidLauncher;)V

    .line 1626
    .line 1627
    .line 1628
    invoke-virtual {v0, v2}, Landroid/view/View;->setOnSystemUiVisibilityChangeListener(Landroid/view/View$OnSystemUiVisibilityChangeListener;)V
    :try_end_65e
    .catchall {:try_start_64e .. :try_end_65e} :catchall_65f

    .line 1629
    .line 1630
    .line 1631
    goto :goto_671

    .line 1632
    :catchall_65f
    move-exception v0

    .line 1633
    const-string v2, "Can\'t create OnSystemUiVisibilityChangeListener, unable to use immersive mode."

    .line 1634
    .line 1635
    const-string v4, "AndroidApplication"

    .line 1636
    .line 1637
    iget v6, v1, Lf/nx3;->DX1:I

    .line 1638
    .line 1639
    const/4 v11, 0x2

    .line 1640
    if-lt v6, v11, :cond_671

    .line 1641
    .line 1642
    iget-object v6, v1, Lf/nx3;->Rt1:Lf/ma3;

    .line 1643
    .line 1644
    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 1645
    .line 1646
    .line 1647
    invoke-static {v4, v2, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 1648
    .line 1649
    .line 1650
    :cond_671
    :goto_671
    invoke-virtual {v1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    .line 1651
    .line 1652
    .line 1653
    move-result-object v0

    .line 1654
    invoke-virtual {v0}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;

    .line 1655
    .line 1656
    .line 1657
    move-result-object v0

    .line 1658
    iget v0, v0, Landroid/content/res/Configuration;->keyboard:I

    .line 1659
    .line 1660
    if-eq v0, v3, :cond_682

    .line 1661
    .line 1662
    iget-object v0, v1, Lf/nx3;->x81:Lf/qa7;

    .line 1663
    .line 1664
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 1665
    .line 1666
    .line 1667
    :cond_682
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    .line 1668
    .line 1669
    if-lt v0, v9, :cond_68c

    .line 1670
    .line 1671
    new-instance v0, Lf/fk6;

    .line 1672
    .line 1673
    invoke-direct {v0, v1}, Lf/fk6;-><init>(Landroid/app/Activity;)V

    .line 1674
    .line 1675
    .line 1676
    goto :goto_691

    .line 1677
    :cond_68c
    new-instance v0, Lf/k8;

    .line 1678
    .line 1679
    invoke-direct {v0, v1}, Lf/k8;-><init>(Landroid/app/Activity;)V

    .line 1680
    .line 1681
    .line 1682
    :goto_691
    iput-object v0, v1, Lf/nx3;->ug1:Lf/t99;

    .line 1683
    .line 1684
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    .line 1685
    .line 1686
    .line 1687
    move-result-object v0

    .line 1688
    invoke-virtual {v0}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    .line 1689
    .line 1690
    .line 1691
    move-result-object v0

    .line 1692
    new-instance v2, Lf/i82;

    .line 1693
    .line 1694
    const/4 v11, 0x2

    .line 1695
    invoke-direct {v2, v11}, Lf/i82;-><init>(I)V

    .line 1696
    .line 1697
    .line 1698
    sget-object v3, Lf/ze2;->Yk0:Ljava/util/WeakHashMap;

    .line 1699
    .line 1700
    invoke-static {v0, v2}, Lf/uy6;->wk1(Landroid/view/View;Lf/uu1;)V

    .line 1701
    .line 1702
    .line 1703
    const-string v0, "CRASH-UUID"

    .line 1704
    .line 1705
    sget-boolean v2, Leu/pokemmo/client/AndroidLauncher;->Mj:Z

    .line 1706
    .line 1707
    if-nez v2, :cond_6ad

    .line 1708
    .line 1709
    goto :goto_717

    .line 1710
    :cond_6ad
    invoke-virtual {v1, v5}, Landroid/app/Activity;->getPreferences(I)Landroid/content/SharedPreferences;

    .line 1711
    .line 1712
    .line 1713
    move-result-object v2

    .line 1714
    const-string v3, "Crash ID not found"

    .line 1715
    .line 1716
    invoke-interface {v2, v0, v3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 1717
    .line 1718
    .line 1719
    move-result-object v3

    .line 1720
    new-instance v4, Ljava/lang/StringBuilder;

    .line 1721
    .line 1722
    const-string v5, "crashlytics-"

    .line 1723
    .line 1724
    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 1725
    .line 1726
    .line 1727
    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;

    .line 1728
    .line 1729
    .line 1730
    move-result-object v5

    .line 1731
    invoke-virtual {v5}, Ljava/util/UUID;->toString()Ljava/lang/String;

    .line 1732
    .line 1733
    .line 1734
    move-result-object v5

    .line 1735
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 1736
    .line 1737
    .line 1738
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1739
    .line 1740
    .line 1741
    move-result-object v4

    .line 1742
    invoke-static {}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->getInstance()Lcom/google/firebase/crashlytics/FirebaseCrashlytics;

    .line 1743
    .line 1744
    .line 1745
    move-result-object v5

    .line 1746
    invoke-virtual {v5, v0, v4}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->setCustomKey(Ljava/lang/String;Ljava/lang/String;)V

    .line 1747
    .line 1748
    .line 1749
    invoke-interface {v2}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    .line 1750
    .line 1751
    .line 1752
    move-result-object v2

    .line 1753
    invoke-interface {v2, v0, v4}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    .line 1754
    .line 1755
    .line 1756
    move-result-object v0

    .line 1757
    invoke-interface {v0}, Landroid/content/SharedPreferences$Editor;->apply()V

    .line 1758
    .line 1759
    .line 1760
    invoke-static {}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->getInstance()Lcom/google/firebase/crashlytics/FirebaseCrashlytics;

    .line 1761
    .line 1762
    .line 1763
    move-result-object v0

    .line 1764
    invoke-virtual {v0}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->didCrashOnPreviousExecution()Z

    .line 1765
    .line 1766
    .line 1767
    move-result v0

    .line 1768
    if-nez v0, :cond_6f1

    .line 1769
    .line 1770
    invoke-static {}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->getInstance()Lcom/google/firebase/crashlytics/FirebaseCrashlytics;

    .line 1771
    .line 1772
    .line 1773
    move-result-object v0

    .line 1774
    invoke-virtual {v0}, Lcom/google/firebase/crashlytics/FirebaseCrashlytics;->deleteUnsentReports()V

    .line 1775
    .line 1776
    .line 1777
    goto :goto_717

    .line 1778
    :cond_6f1
    sget v0, Lf/j80;->my1:I

    .line 1779
    .line 1780
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 1781
    .line 1782
    .line 1783
    move-result-object v9

    .line 1784
    sget-object v5, Lf/p37;->IT:Lf/zy3;

    .line 1785
    .line 1786
    sget v0, Lf/j80;->c02:I

    .line 1787
    .line 1788
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 1789
    .line 1790
    .line 1791
    move-result-object v10

    .line 1792
    new-instance v6, Lf/lr6;

    .line 1793
    .line 1794
    invoke-direct {v6, v1, v3}, Lf/lr6;-><init>(Leu/pokemmo/client/AndroidLauncher;Ljava/lang/String;)V

    .line 1795
    .line 1796
    .line 1797
    new-instance v8, Lf/fn3;

    .line 1798
    .line 1799
    const/4 v2, 0x4

    .line 1800
    invoke-direct {v8, v2}, Lf/fn3;-><init>(I)V

    .line 1801
    .line 1802
    .line 1803
    iget-object v0, v5, Lf/zy3;->TY0:Ljava/lang/Object;

    .line 1804
    .line 1805
    check-cast v0, Leu/pokemmo/client/AndroidLauncher;

    .line 1806
    .line 1807
    new-instance v4, Lf/gq0;

    .line 1808
    .line 1809
    const/4 v7, 0x0

    .line 1810
    invoke-direct/range {v4 .. v10}, Lf/gq0;-><init>(Lf/zy3;Ljava/lang/Runnable;ZLjava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V

    .line 1811
    .line 1812
    .line 1813
    invoke-virtual {v0, v4}, Landroid/app/Activity;->runOnUiThread(Ljava/lang/Runnable;)V

    .line 1814
    .line 1815
    .line 1816
    :goto_717
    return-void

    .line 1817
    :catchall_718
    move-exception v0

    .line 1818
    :try_start_719
    monitor-exit v6
    :try_end_71a
    .catchall {:try_start_719 .. :try_end_71a} :catchall_718

    .line 1819
    throw v0

    .line 1820
    nop

    .line 1821
    :array_71c
    .array-data 4
        0x1f40
        0x2b11
        0x3e80
        0x5622
        0x7d00
        0x93a8
        0xac18
        0xac44
        0xb892
        0xbb80
    .end array-data
.end method

.method public final onDestroy()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    const/4 v0, 0x0

    .line 7
    sput-object v0, Lf/wn5;->b5:Lf/wn5;

    .line 8
    .line 9
    sput-object v0, Lf/r41;->XG:Lf/r41;

    .line 10
    .line 11
    invoke-super {p0}, Lf/nx3;->onDestroy()V

    .line 12
    .line 13
    .line 14
    return-void
.end method

.method public final onLowMemory()V
    .registers 3

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    const-string v1, "onLowMemory"

    .line 4
    .line 5
    invoke-interface {v0, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    invoke-super {p0}, Landroid/app/Activity;->onLowMemory()V

    .line 9
    .line 10
    .line 11
    return-void
.end method

.method public final onPause()V
    .registers 2

    .line 1
    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    .line 2
    .line 3
    .line 4
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 5
    .line 6
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 7
    .line 8
    .line 9
    invoke-super {p0}, Lf/nx3;->onPause()V

    .line 10
    .line 11
    .line 12
    return-void
.end method

.method public final onPostResume()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0}, Landroid/app/Activity;->onPostResume()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onRequestPermissionsResult(I[Ljava/lang/String;[I)V
    .registers 4

    .line 1
    iget-object p2, p0, Leu/pokemmo/client/AndroidLauncher;->fQ0:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {p2, p1}, Lf/x44;->iu0(I)I

    .line 4
    .line 5
    .line 6
    move-result p1

    .line 7
    if-ltz p1, :cond_10

    .line 8
    .line 9
    iget-object p3, p2, Lf/k89;->Y7:[Ljava/lang/Object;

    .line 10
    .line 11
    aget-object p3, p3, p1

    .line 12
    .line 13
    invoke-virtual {p2, p1}, Lf/k89;->Eu(I)V

    .line 14
    .line 15
    .line 16
    goto :goto_11

    .line 17
    :cond_10
    const/4 p3, 0x0

    .line 18
    :goto_11
    check-cast p3, Ljava/lang/Runnable;

    .line 19
    .line 20
    if-eqz p3, :cond_18

    .line 21
    .line 22
    invoke-interface {p3}, Ljava/lang/Runnable;->run()V

    .line 23
    .line 24
    .line 25
    :cond_18
    return-void
.end method

.method public final onRestart()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0}, Landroid/app/Activity;->onRestart()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onRestoreInstanceState(Landroid/os/Bundle;)V
    .registers 3

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0, p1}, Landroid/app/Activity;->onRestoreInstanceState(Landroid/os/Bundle;)V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onResume()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0}, Lf/nx3;->onResume()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onStart()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0}, Landroid/app/Activity;->onStart()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onStop()V
    .registers 2

    .line 1
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 4
    .line 5
    .line 6
    invoke-super {p0}, Landroid/app/Activity;->onStop()V

    .line 7
    .line 8
    .line 9
    return-void
.end method

.method public final onTrimMemory(I)V
    .registers 5

    .line 1
    const-string v0, "onTrimMemory({})"

    .line 2
    .line 3
    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 4
    .line 5
    .line 6
    move-result-object v1

    .line 7
    sget-object v2, Leu/pokemmo/client/AndroidLauncher;->YA1:Lf/xv7;

    .line 8
    .line 9
    invoke-interface {v2, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 10
    .line 11
    .line 12
    const/16 v0, 0x3c

    .line 13
    .line 14
    if-eq p1, v0, :cond_28

    .line 15
    .line 16
    const/16 v0, 0x50

    .line 17
    .line 18
    if-eq p1, v0, :cond_14

    .line 19
    .line 20
    goto :goto_3b

    .line 21
    :cond_14
    sget-object v0, Lf/p37;->se:Lf/qr3;

    .line 22
    .line 23
    if-eqz v0, :cond_3b

    .line 24
    .line 25
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->X4:Lf/a97;

    .line 26
    .line 27
    invoke-virtual {v0}, Lf/a97;->MJ1()Z

    .line 28
    .line 29
    .line 30
    move-result v0

    .line 31
    if-eqz v0, :cond_3b

    .line 32
    .line 33
    sget-object v0, Lf/p37;->se:Lf/qr3;

    .line 34
    .line 35
    const-string v1, "Very low memory detected."

    .line 36
    .line 37
    invoke-virtual {v0, v1}, Lf/eb5;->sJ1(Ljava/lang/String;)V

    .line 38
    .line 39
    .line 40
    goto :goto_3b

    .line 41
    :cond_28
    sget-object v0, Lf/p37;->se:Lf/qr3;

    .line 42
    .line 43
    if-eqz v0, :cond_3b

    .line 44
    .line 45
    sget-object v0, Leu/pokemmo/client/AndroidLauncher;->MA0:Lf/a97;

    .line 46
    .line 47
    invoke-virtual {v0}, Lf/a97;->MJ1()Z

    .line 48
    .line 49
    .line 50
    move-result v0

    .line 51
    if-eqz v0, :cond_3b

    .line 52
    .line 53
    sget-object v0, Lf/p37;->se:Lf/qr3;

    .line 54
    .line 55
    const-string v1, "Low memory detected."

    .line 56
    .line 57
    invoke-virtual {v0, v1}, Lf/eb5;->sJ1(Ljava/lang/String;)V

    .line 58
    .line 59
    .line 60
    :cond_3b
    :goto_3b
    invoke-super {p0, p1}, Landroid/app/Activity;->onTrimMemory(I)V

    .line 61
    .line 62
    .line 63
    return-void
.end method

# MonMMO-EX: copy the bundled theme mod out of assets into the folder the client actually scans.
#
# f/Kg0 builds its handle for "data/mods/" with f/Zr1.G81 - external storage - while our zip ships
# inside the APK's assets, a different root entirely. Without this the mod is never listed, and both
# client.mods.enabled_mods and client.ui.theme point at things that do not exist.
#
# f/qJ.zY0() is exists() (it calls File.exists); SM1(InputStream) writes the stream to the file and
# its td1() creates the parent directory first, so nothing else has to prepare the folder. Any
# failure here is swallowed: a missing theme must never stop the client from starting.
.method public static monmmoInstallBundledMod(Landroid/content/Context;)V
    .locals 4

    const-string v0, "data/mods/monmmo-theme.zip"

    new-instance v1, Lf/Vk;

    sget-object v2, Lf/Zr1;->G81:Lf/Zr1;

    invoke-direct {v1, v0, v2}, Lf/Vk;-><init>(Ljava/lang/String;Lf/Zr1;)V

    invoke-virtual {v1}, Lf/qJ;->zY0()Z

    move-result v2

    if-eqz v2, :cond_copy

    return-void

    :cond_copy
    :try_start_0
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v2

    invoke-virtual {v2, v0}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v3

    invoke-virtual {v1, v3}, Lf/qJ;->SM1(Ljava/io/InputStream;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    return-void
.end method
