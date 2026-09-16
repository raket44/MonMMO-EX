.class public final Lf/nb4;
.super Ljava/lang/Object;

# interfaces
.implements Ljava/lang/Runnable;


# static fields
.field public static final Kn1:Lf/nb4;


# instance fields
.field public AK:I

.field public final Ap:Lf/a97;

.field public final Lh:Ljava/util/ArrayList;

.field public Pc:Lf/b38;

.field public final eQ0:Lf/a97;

.field public fN0:I

.field public or0:Lf/b38;

.field public final rF:Ljava/util/ArrayList;

.field public uc0:Lf/gl0;

.field public vq1:B

.field public yZ:Lf/di;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 1
    new-instance v0, Lf/nb4;

    .line 2
    .line 3
    invoke-direct {v0}, Lf/nb4;-><init>()V

    .line 4
    .line 5
    .line 6
    sput-object v0, Lf/nb4;->Kn1:Lf/nb4;

    .line 7
    .line 8
    return-void
.end method

.method public constructor <init>()V
    .registers 3

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    new-instance v0, Lf/a97;

    .line 5
    .line 6
    const/16 v1, 0x7530

    .line 7
    .line 8
    invoke-direct {v0, v1}, Lf/a97;-><init>(I)V

    .line 9
    .line 10
    .line 11
    iput-object v0, p0, Lf/nb4;->eQ0:Lf/a97;

    .line 12
    .line 13
    new-instance v0, Lf/a97;

    .line 14
    .line 15
    invoke-direct {v0, v1}, Lf/a97;-><init>(I)V

    .line 16
    .line 17
    .line 18
    iput-object v0, p0, Lf/nb4;->Ap:Lf/a97;

    .line 19
    .line 20
    const v0, 0x61cf9980

    .line 21
    .line 22
    .line 23
    iput v0, p0, Lf/nb4;->fN0:I

    .line 24
    .line 25
    sget-object v0, Lf/di;->bL:Lf/di;

    .line 26
    .line 27
    iput-object v0, p0, Lf/nb4;->yZ:Lf/di;

    .line 28
    .line 29
    sget-object v0, Lf/gl0;->qj:Lf/gl0;

    .line 30
    .line 31
    iput-object v0, p0, Lf/nb4;->uc0:Lf/gl0;

    .line 32
    .line 33
    const/4 v0, -0x1

    .line 34
    iput-byte v0, p0, Lf/nb4;->vq1:B

    .line 35
    .line 36
    sget-object v0, Lf/b38;->lpT2:Lf/b38;

    .line 37
    .line 38
    iput-object v0, p0, Lf/nb4;->or0:Lf/b38;

    .line 39
    .line 40
    iput-object v0, p0, Lf/nb4;->Pc:Lf/b38;

    .line 41
    .line 42
    new-instance v0, Ljava/util/ArrayList;

    .line 43
    .line 44
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 45
    .line 46
    .line 47
    iput-object v0, p0, Lf/nb4;->rF:Ljava/util/ArrayList;

    .line 48
    .line 49
    new-instance v0, Ljava/util/ArrayList;

    .line 50
    .line 51
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 52
    .line 53
    .line 54
    iput-object v0, p0, Lf/nb4;->Lh:Ljava/util/ArrayList;

    .line 55
    .line 56
    return-void
.end method


# virtual methods
.method public final D60()Lf/gl0;
    .registers 4

    .line 1
    invoke-virtual {p0}, Lf/nb4;->qN()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    # MonMMO-EX: the weekday is the real calendar one in the server's zone, not the in-game day
    # counter at 4x (which reset to Sunday every real midnight). qN()/4 = real seconds since the
    # join anchor, which the server sets to a Sunday midnight (WorldClock.dayStartSecond), so
    # real days since it mod 7 is the calendar weekday, Sunday 0.
    div-int/lit8 v0, v0, 0x4

    const v1, 0x15180

    div-int/2addr v0, v1

    rem-int/lit8 v0, v0, 0x7

    .line 13
    int-to-byte v0, v0

    .line 14
    sget-object v1, Lf/gl0;->Ce:Lf/k33;

    .line 15
    .line 16
    invoke-virtual {v1, v0}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 17
    .line 18
    .line 19
    move-result-object v1

    .line 20
    check-cast v1, Lf/gl0;

    .line 21
    .line 22
    const-class v2, Lf/gl0;

    .line 23
    .line 24
    invoke-static {v1, v2, v0}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 25
    .line 26
    .line 27
    return-object v1
.end method

.method public final Da0()Lf/b38;
    .registers 6

    .line 1
    iget-object v0, p0, Lf/nb4;->Pc:Lf/b38;

    .line 2
    .line 3
    sget-object v1, Lf/b38;->lpT2:Lf/b38;

    .line 4
    .line 5
    if-eq v0, v1, :cond_7

    .line 6
    .line 7
    return-object v0

    .line 8
    :cond_7
    iget-object v0, p0, Lf/nb4;->or0:Lf/b38;

    .line 9
    .line 10
    if-eq v0, v1, :cond_1d

    .line 11
    .line 12
    iget-object v0, p0, Lf/nb4;->Ap:Lf/a97;

    .line 13
    .line 14
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 15
    .line 16
    .line 17
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 18
    .line 19
    .line 20
    move-result-wide v1

    .line 21
    iget-wide v3, v0, Lf/a97;->O41:J

    .line 22
    .line 23
    cmp-long v0, v1, v3

    .line 24
    .line 25
    if-gez v0, :cond_1d

    .line 26
    .line 27
    iget-object v0, p0, Lf/nb4;->or0:Lf/b38;

    .line 28
    .line 29
    return-object v0

    .line 30
    :cond_1d
    invoke-virtual {p0}, Lf/nb4;->W8()V

    .line 31
    .line 32
    .line 33
    iget-object v0, p0, Lf/nb4;->or0:Lf/b38;

    .line 34
    .line 35
    return-object v0
.end method

.method public final Pm1()I
    .registers 3

    .line 1
    iget-byte v0, p0, Lf/nb4;->vq1:B

    .line 2
    .line 3
    if-lez v0, :cond_9

    .line 4
    .line 5
    const/16 v1, 0x19

    .line 6
    .line 7
    if-ge v0, v1, :cond_9

    .line 8
    .line 9
    return v0

    .line 10
    :cond_9
    invoke-virtual {p0}, Lf/nb4;->qN()I

    .line 11
    .line 12
    .line 13
    move-result v0

    .line 14
    const v1, 0x15180

    .line 15
    .line 16
    .line 17
    rem-int/2addr v0, v1

    .line 18
    div-int/lit16 v0, v0, 0xe10

    .line 19
    .line 20
    return v0
.end method

.method public final W8()V
    .registers 3

    .line 1
    iget-object v0, p0, Lf/nb4;->Ap:Lf/a97;

    .line 2
    .line 3
    invoke-virtual {v0}, Lf/a97;->Wr0()Z

    .line 4
    .line 5
    .line 6
    new-instance v0, Ljava/util/GregorianCalendar;

    .line 7
    .line 8
    const-string v1, "UTC"

    .line 9
    .line 10
    invoke-static {v1}, Lj$/util/DesugarTimeZone;->getTimeZone(Ljava/lang/String;)Ljava/util/TimeZone;

    .line 11
    .line 12
    .line 13
    move-result-object v1

    .line 14
    invoke-direct {v0, v1}, Ljava/util/GregorianCalendar;-><init>(Ljava/util/TimeZone;)V

    .line 15
    .line 16
    .line 17
    const/4 v1, 0x2

    .line 18
    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    .line 19
    .line 20
    .line 21
    move-result v0

    .line 22
    rem-int/lit8 v0, v0, 0x4

    .line 23
    .line 24
    int-to-byte v0, v0

    .line 25
    invoke-static {v0}, Lf/b38;->eA0(B)Lf/b38;

    .line 26
    .line 27
    .line 28
    move-result-object v0

    .line 29
    iput-object v0, p0, Lf/nb4;->or0:Lf/b38;

    .line 30
    .line 31
    return-void
.end method

.method public final Wn0(Lf/di;)V
    .registers 3

    .line 1
    iget-object v0, p0, Lf/nb4;->yZ:Lf/di;

    .line 2
    .line 3
    if-ne v0, p1, :cond_5

    .line 4
    .line 5
    return-void

    .line 6
    :cond_5
    iput-object p1, p0, Lf/nb4;->yZ:Lf/di;

    .line 7
    .line 8
    invoke-virtual {p0}, Lf/nb4;->e90()V

    .line 9
    .line 10
    .line 11
    return-void
.end method

.method public final ah1()Lf/di;
    .registers 3

    .line 1
    invoke-virtual {p0}, Lf/nb4;->Pm1()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    const/4 v1, 0x6

    .line 6
    if-lt v0, v1, :cond_e

    .line 7
    .line 8
    const/16 v1, 0x11

    .line 9
    .line 10
    if-ge v0, v1, :cond_e

    .line 11
    .line 12
    sget-object v0, Lf/di;->bL:Lf/di;

    .line 13
    .line 14
    return-object v0

    .line 15
    :cond_e
    sget-object v0, Lf/di;->rT1:Lf/di;

    .line 16
    .line 17
    return-object v0
.end method

.method public final e90()V
    .registers 6

    .line 1
    iget-object v0, p0, Lf/nb4;->rF:Ljava/util/ArrayList;

    .line 2
    .line 3
    monitor-enter v0

    .line 4
    :try_start_3
    iget-object v1, p0, Lf/nb4;->rF:Ljava/util/ArrayList;

    .line 5
    .line 6
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 7
    .line 8
    .line 9
    move-result v2

    .line 10
    const/4 v3, 0x0

    .line 11
    :goto_a
    if-ge v3, v2, :cond_1a

    .line 12
    .line 13
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 14
    .line 15
    .line 16
    move-result-object v4

    .line 17
    add-int/lit8 v3, v3, 0x1

    .line 18
    .line 19
    check-cast v4, Ljava/lang/Runnable;

    .line 20
    .line 21
    invoke-interface {v4}, Ljava/lang/Runnable;->run()V

    .line 22
    .line 23
    .line 24
    goto :goto_a

    .line 25
    :catchall_18
    move-exception v1

    .line 26
    goto :goto_1c

    .line 27
    :cond_1a
    monitor-exit v0

    .line 28
    return-void

    .line 29
    :goto_1c
    monitor-exit v0
    :try_end_1d
    .catchall {:try_start_3 .. :try_end_1d} :catchall_18

    .line 30
    throw v1
.end method

.method public final jP(Lf/b38;)V
    .registers 3

    .line 1
    if-nez p1, :cond_4

    .line 2
    .line 3
    sget-object p1, Lf/b38;->lpT2:Lf/b38;

    .line 4
    .line 5
    :cond_4
    invoke-virtual {p0}, Lf/nb4;->Da0()Lf/b38;

    .line 6
    .line 7
    .line 8
    move-result-object v0

    .line 9
    if-eq v0, p1, :cond_c

    .line 10
    .line 11
    const/4 v0, 0x1

    .line 12
    goto :goto_d

    .line 13
    :cond_c
    const/4 v0, 0x0

    .line 14
    :goto_d
    iput-object p1, p0, Lf/nb4;->Pc:Lf/b38;

    .line 15
    .line 16
    if-eqz v0, :cond_14

    .line 17
    .line 18
    invoke-virtual {p0}, Lf/nb4;->e90()V

    .line 19
    .line 20
    .line 21
    :cond_14
    return-void
.end method

.method public final kc0()Lf/di;
    .registers 3

    .line 1
    invoke-virtual {p0}, Lf/nb4;->Pm1()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    const/4 v1, 0x6

    .line 6
    if-ge v0, v1, :cond_a

    .line 7
    .line 8
    sget-object v0, Lf/di;->AH0:Lf/di;

    .line 9
    .line 10
    return-object v0

    .line 11
    :cond_a
    const/16 v1, 0xb

    .line 12
    .line 13
    if-ge v0, v1, :cond_11

    .line 14
    .line 15
    sget-object v0, Lf/di;->Wx1:Lf/di;

    .line 16
    .line 17
    return-object v0

    .line 18
    :cond_11
    const/16 v1, 0x12

    .line 19
    .line 20
    if-ge v0, v1, :cond_18

    .line 21
    .line 22
    sget-object v0, Lf/di;->bL:Lf/di;

    .line 23
    .line 24
    return-object v0

    .line 25
    :cond_18
    const/16 v1, 0x15

    .line 26
    .line 27
    if-ge v0, v1, :cond_1f

    .line 28
    .line 29
    sget-object v0, Lf/di;->mr1:Lf/di;

    .line 30
    .line 31
    return-object v0

    .line 32
    :cond_1f
    sget-object v0, Lf/di;->rT1:Lf/di;

    .line 33
    .line 34
    return-object v0
.end method

.method public final qN()I
    .registers 5

    .line 1
    iget-byte v0, p0, Lf/nb4;->vq1:B

    .line 2
    .line 3
    if-lez v0, :cond_b

    .line 4
    .line 5
    const/16 v1, 0x19

    .line 6
    .line 7
    if-ge v0, v1, :cond_b

    .line 8
    .line 9
    mul-int/lit16 v0, v0, 0xe10

    .line 10
    .line 11
    return v0

    .line 12
    :cond_b
    iget v0, p0, Lf/nb4;->fN0:I

    .line 13
    .line 14
    if-lez v0, :cond_20

    .line 15
    .line 16
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 17
    .line 18
    .line 19
    move-result-wide v0

    .line 20
    const-wide/16 v2, 0x3e8

    .line 21
    .line 22
    div-long/2addr v0, v2

    .line 23
    long-to-int v1, v0

    .line 24
    iget v0, p0, Lf/nb4;->AK:I

    .line 25
    .line 26
    sub-int/2addr v1, v0

    .line 27
    iget v0, p0, Lf/nb4;->fN0:I

    .line 28
    .line 29
    sub-int/2addr v1, v0

    .line 30
    mul-int/lit8 v1, v1, 0x4

    .line 31
    .line 32
    return v1

    .line 33
    :cond_20
    new-instance v0, Ljava/lang/IllegalStateException;

    .line 34
    .line 35
    invoke-direct {v0}, Ljava/lang/IllegalStateException;-><init>()V

    .line 36
    .line 37
    .line 38
    throw v0
.end method

.method public final run()V
    .registers 6

    .line 1
    invoke-virtual {p0}, Lf/nb4;->Pm1()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    const/4 v1, 0x4

    .line 6
    if-ge v0, v1, :cond_d

    .line 7
    .line 8
    sget-object v0, Lf/di;->AH0:Lf/di;

    .line 9
    .line 10
    :goto_9
    invoke-virtual {p0, v0}, Lf/nb4;->Wn0(Lf/di;)V

    .line 11
    .line 12
    .line 13
    goto :goto_25

    .line 14
    :cond_d
    const/16 v1, 0xb

    .line 15
    .line 16
    if-ge v0, v1, :cond_14

    .line 17
    .line 18
    sget-object v0, Lf/di;->Wx1:Lf/di;

    .line 19
    .line 20
    goto :goto_9

    .line 21
    :cond_14
    const/16 v1, 0x12

    .line 22
    .line 23
    if-ge v0, v1, :cond_1b

    .line 24
    .line 25
    sget-object v0, Lf/di;->bL:Lf/di;

    .line 26
    .line 27
    goto :goto_9

    .line 28
    :cond_1b
    const/16 v1, 0x15

    .line 29
    .line 30
    if-ge v0, v1, :cond_22

    .line 31
    .line 32
    sget-object v0, Lf/di;->mr1:Lf/di;

    .line 33
    .line 34
    goto :goto_9

    .line 35
    :cond_22
    sget-object v0, Lf/di;->rT1:Lf/di;

    .line 36
    .line 37
    goto :goto_9

    .line 38
    :goto_25
    iget-object v0, p0, Lf/nb4;->uc0:Lf/gl0;

    .line 39
    .line 40
    invoke-virtual {p0}, Lf/nb4;->D60()Lf/gl0;

    .line 41
    .line 42
    .line 43
    move-result-object v1

    .line 44
    if-eq v0, v1, :cond_54

    .line 45
    .line 46
    invoke-virtual {p0}, Lf/nb4;->D60()Lf/gl0;

    .line 47
    .line 48
    .line 49
    move-result-object v0

    .line 50
    iput-object v0, p0, Lf/nb4;->uc0:Lf/gl0;

    .line 51
    .line 52
    invoke-virtual {p0}, Lf/nb4;->e90()V

    .line 53
    .line 54
    .line 55
    iget-object v0, p0, Lf/nb4;->Lh:Ljava/util/ArrayList;

    .line 56
    .line 57
    monitor-enter v0

    .line 58
    :try_start_39
    iget-object v1, p0, Lf/nb4;->Lh:Ljava/util/ArrayList;

    .line 59
    .line 60
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 61
    .line 62
    .line 63
    move-result v2

    .line 64
    const/4 v3, 0x0

    .line 65
    :goto_40
    if-ge v3, v2, :cond_50

    .line 66
    .line 67
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 68
    .line 69
    .line 70
    move-result-object v4

    .line 71
    add-int/lit8 v3, v3, 0x1

    .line 72
    .line 73
    check-cast v4, Ljava/lang/Runnable;

    .line 74
    .line 75
    invoke-interface {v4}, Ljava/lang/Runnable;->run()V

    .line 76
    .line 77
    .line 78
    goto :goto_40

    .line 79
    :catchall_4e
    move-exception v1

    .line 80
    goto :goto_52

    .line 81
    :cond_50
    monitor-exit v0

    .line 82
    goto :goto_54

    .line 83
    :goto_52
    monitor-exit v0
    :try_end_53
    .catchall {:try_start_39 .. :try_end_53} :catchall_4e

    .line 84
    throw v1

    .line 85
    :cond_54
    :goto_54
    invoke-virtual {p0}, Lf/nb4;->W8()V

    .line 86
    .line 87
    .line 88
    return-void
.end method

.method public final sc0()I
    .registers 2

    .line 1
    invoke-virtual {p0}, Lf/nb4;->qN()I

    .line 2
    .line 3
    .line 4
    move-result v0

    .line 5
    rem-int/lit16 v0, v0, 0xe10

    .line 6
    .line 7
    div-int/lit8 v0, v0, 0x3c

    .line 8
    .line 9
    return v0
.end method
