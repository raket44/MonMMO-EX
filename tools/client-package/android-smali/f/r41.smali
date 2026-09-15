.class public final Lf/r41;
.super Ljava/lang/Object;

# interfaces
.implements Lf/pu0;


# static fields
.field public static XG:Lf/r41;


# instance fields
.field public C60:Lf/m39;

.field public CF:[Lf/m39;

.field public Ck0:Lf/m39;

.field public Ck1:Lcom/badlogic/gdx/graphics/Texture;

.field public Cp0:Lf/m39;

.field public DG1:Ljava/lang/String;

.field public Dy0:[Lf/m39;

.field public EM0:Lf/m39;

.field public FW1:Lf/m39;

.field public Fa:Lf/m39;

.field public HC1:Lf/ga8;

.field public Ic:Lcom/badlogic/gdx/graphics/Texture;

.field public If1:[Lf/m39;

.field public Iq1:[Lf/m39;

.field public J50:Lf/m39;

.field public JA0:Lf/m39;

.field public JC0:[Lf/m39;

.field public K6:[Lf/m39;

.field public KX:Lf/m39;

.field public Mg:Lf/m39;

.field public Mu0:Lf/m39;

.field public Nf0:Lf/m39;

.field public Nl1:[Lf/m39;

.field public OJ1:Lf/m39;

.field public Ph0:Lf/m39;

.field public QZ0:Lcom/badlogic/gdx/graphics/Texture;

.field public Rw:Lf/dk2;

.field public SP:Lf/m39;

.field public Sf1:[Lf/m39;

.field public Sk:Lf/m39;

.field public TG1:Ljava/util/HashMap;

.field public U60:[Lf/m39;

.field public Ua0:Lf/m39;

.field public Uq1:Lf/m39;

.field public V9:Lf/m39;

.field public Y1:Lf/m39;

.field public Yi:Lf/m39;

.field public ZM:[Lf/m39;

.field public Zg0:Lf/m39;

.field public bE:Lf/m39;

.field public cQ:Lf/m39;

.field public cm1:Lf/m39;

.field public cw:[Lcom/badlogic/gdx/graphics/Texture;

.field public eo:Lcom/badlogic/gdx/graphics/Texture;

.field public fA:Lf/m39;

.field public fW0:[Lf/m39;

.field public h5:Lf/m39;

.field public hS0:Lf/m39;

.field public iO0:Lf/m39;

.field public iY0:Lf/m39;

.field public ib:Lf/m39;

.field public iw1:[Lf/m39;

.field public lh1:[Lf/m39;

.field public lq0:[Lf/m39;

.field public lt:[Lf/m39;

.field public of0:Lf/m39;

.field public qV1:Lf/m39;

.field public r30:Lf/m39;

.field public rB1:Lf/m39;

.field public rV1:Lf/m39;

.field public rz:Lf/v99;

.field public sW1:Lf/m39;

.field public tH:Lf/m39;

.field public tV1:Lf/m39;

.field public uS0:Lf/m39;

.field public vp:[Lf/m39;

.field public x61:Lf/m39;

.field public xP:Lf/m39;

.field public xf0:Lf/m39;

.field public xi:Lf/m39;

.field public xr0:Lf/m39;

# MonMMO-EX: the evolution tab's battle form-change symbols (atlas page monmmo-evo-symbols), loaded
# with the clock badges in f/rm4 - Mega Evolution, and the alpha / omega of Primal Kyogre / Groudon.
.field public mmxMega:Lf/m39;

.field public mmxAlpha:Lf/m39;

.field public mmxOmega:Lf/m39;


# direct methods
.method public static a40()Lf/r41;
    .registers 2

    .line 1
    sget-object v0, Lf/r41;->XG:Lf/r41;

    .line 2
    .line 3
    if-nez v0, :cond_16

    .line 4
    .line 5
    new-instance v0, Lf/r41;

    .line 6
    .line 7
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 8
    .line 9
    .line 10
    new-instance v1, Ljava/util/HashMap;

    .line 11
    .line 12
    invoke-direct {v1}, Ljava/util/HashMap;-><init>()V

    .line 13
    .line 14
    .line 15
    iput-object v1, v0, Lf/r41;->TG1:Ljava/util/HashMap;

    .line 16
    .line 17
    const-string v1, ""

    .line 18
    .line 19
    iput-object v1, v0, Lf/r41;->DG1:Ljava/lang/String;

    .line 20
    .line 21
    sput-object v0, Lf/r41;->XG:Lf/r41;

    .line 22
    .line 23
    :cond_16
    sget-object v0, Lf/r41;->XG:Lf/r41;

    .line 24
    .line 25
    return-object v0
.end method

.method public static lC1()[Lcom/badlogic/gdx/graphics/Texture;
    .registers 6

    .line 1
    new-instance v0, Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 4
    .line 5
    .line 6
    const/4 v1, 0x0

    .line 7
    const/4 v2, 0x0

    .line 8
    :goto_7
    const/16 v3, 0x64

    .line 9
    .line 10
    if-ge v2, v3, :cond_45

    .line 11
    .line 12
    new-instance v3, Ljava/lang/StringBuilder;

    .line 13
    .line 14
    const-string v4, "data/sprites/textures/bg_"

    .line 15
    .line 16
    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 17
    .line 18
    .line 19
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 20
    .line 21
    .line 22
    move-result-object v4

    .line 23
    const/4 v5, 0x1

    .line 24
    new-array v5, v5, [Ljava/lang/Object;

    .line 25
    .line 26
    aput-object v4, v5, v1

    .line 27
    .line 28
    const-string v4, "%02d"

    .line 29
    .line 30
    invoke-static {v4, v5}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 31
    .line 32
    .line 33
    move-result-object v4

    .line 34
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 35
    .line 36
    .line 37
    const-string v4, ".png"

    .line 38
    .line 39
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 40
    .line 41
    .line 42
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 43
    .line 44
    .line 45
    move-result-object v3

    .line 46
    sget-object v4, Lf/p37;->K2:Lf/px0;

    .line 47
    .line 48
    invoke-virtual {v4, v3}, Lf/px0;->yZ0(Ljava/lang/String;)Lf/v9;

    .line 49
    .line 50
    .line 51
    move-result-object v3

    .line 52
    invoke-virtual {v3}, Lf/v9;->zz1()Lf/z46;

    .line 53
    .line 54
    .line 55
    move-result-object v3

    .line 56
    if-nez v3, :cond_3a

    .line 57
    .line 58
    goto :goto_45

    .line 59
    :cond_3a
    new-instance v4, Lcom/badlogic/gdx/graphics/Texture;

    .line 60
    .line 61
    invoke-direct {v4, v3}, Lcom/badlogic/gdx/graphics/Texture;-><init>(Lf/z46;)V

    .line 62
    .line 63
    .line 64
    invoke-virtual {v0, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 65
    .line 66
    .line 67
    add-int/lit8 v2, v2, 0x1

    .line 68
    .line 69
    goto :goto_7

    .line 70
    :cond_45
    :goto_45
    new-array v1, v1, [Lcom/badlogic/gdx/graphics/Texture;

    .line 71
    .line 72
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 73
    .line 74
    .line 75
    move-result-object v0

    .line 76
    check-cast v0, [Lcom/badlogic/gdx/graphics/Texture;

    .line 77
    .line 78
    return-object v0
.end method

.method public static lp0(B)I
    .registers 2

    .line 1
    const/16 v0, -0x80

    .line 2
    .line 3
    if-eq p0, v0, :cond_23

    .line 4
    .line 5
    const/16 v0, 0x10

    .line 6
    .line 7
    if-eq p0, v0, :cond_21

    .line 8
    .line 9
    const/16 v0, 0x20

    .line 10
    .line 11
    if-eq p0, v0, :cond_1f

    .line 12
    .line 13
    const/16 v0, 0x40

    .line 14
    .line 15
    if-eq p0, v0, :cond_1d

    .line 16
    .line 17
    const/4 v0, 0x7

    .line 18
    if-eq p0, v0, :cond_1b

    .line 19
    .line 20
    const/16 v0, 0x8

    .line 21
    .line 22
    if-eq p0, v0, :cond_19

    .line 23
    .line 24
    const/4 p0, 0x5

    .line 25
    return p0

    .line 26
    :cond_19
    const/4 p0, 0x3

    .line 27
    return p0

    .line 28
    :cond_1b
    const/4 p0, 0x4

    .line 29
    return p0

    .line 30
    :cond_1d
    const/4 p0, 0x1

    .line 31
    return p0

    .line 32
    :cond_1f
    const/4 p0, 0x0

    .line 33
    return p0

    .line 34
    :cond_21
    const/4 p0, 0x2

    .line 35
    return p0

    .line 36
    :cond_23
    const/4 p0, 0x6

    .line 37
    return p0
.end method


# virtual methods
.method public final GC()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->hS0:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final Kc1()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->Zg0:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final LQ0(B)Lf/m39;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/r41;->fW0:[Lf/m39;

    .line 2
    .line 3
    aget-object p1, v0, p1

    .line 4
    .line 5
    return-object p1
.end method

.method public final M3()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->Sk:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final Nm0(ILjava/lang/String;)Lf/m39;
    .registers 9

    .line 1
    iget-object v0, p0, Lf/r41;->Rw:Lf/dk2;

    .line 2
    .line 3
    const/4 v1, 0x0

    .line 4
    if-eqz v0, :cond_3b

    .line 5
    .line 6
    iget-object v2, v0, Lf/dk2;->pc:Lf/z46;

    .line 7
    .line 8
    new-instance v3, Ljava/lang/StringBuilder;

    .line 9
    .line 10
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    .line 11
    .line 12
    .line 13
    invoke-virtual {v3, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 14
    .line 15
    .line 16
    const-string p2, "_"

    .line 17
    .line 18
    invoke-virtual {v3, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 19
    .line 20
    .line 21
    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 22
    .line 23
    .line 24
    const-string p1, ".png"

    .line 25
    .line 26
    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 27
    .line 28
    .line 29
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 30
    .line 31
    .line 32
    move-result-object p1

    .line 33
    invoke-virtual {v2, p1}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 34
    .line 35
    .line 36
    move-result-object p1

    .line 37
    invoke-virtual {p1}, Lf/z46;->hz1()Z

    .line 38
    .line 39
    .line 40
    move-result p2

    .line 41
    if-nez p2, :cond_2b

    .line 42
    .line 43
    goto :goto_5d

    .line 44
    :cond_2b
    new-instance p2, Lcom/badlogic/gdx/graphics/Texture;

    .line 45
    .line 46
    invoke-direct {p2, p1}, Lcom/badlogic/gdx/graphics/Texture;-><init>(Lf/z46;)V

    .line 47
    .line 48
    .line 49
    iget-object p1, v0, Lf/dk2;->op:Ljava/util/ArrayList;

    .line 50
    .line 51
    invoke-virtual {p1, p2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 52
    .line 53
    .line 54
    new-instance v1, Lf/m39;

    .line 55
    .line 56
    invoke-direct {v1, p2}, Lf/m39;-><init>(Lcom/badlogic/gdx/graphics/Texture;)V

    .line 57
    .line 58
    .line 59
    goto :goto_5d

    .line 60
    :cond_3b
    iget-object v0, p0, Lf/r41;->HC1:Lf/ga8;

    .line 61
    .line 62
    if-eqz v0, :cond_5d

    .line 63
    .line 64
    iget-object v0, v0, Lf/ga8;->Cp0:Lf/rh6;

    .line 65
    .line 66
    iget v2, v0, Lf/rh6;->Mm:I

    .line 67
    .line 68
    const/4 v3, 0x0

    .line 69
    :goto_44
    if-ge v3, v2, :cond_5d

    .line 70
    .line 71
    invoke-virtual {v0, v3}, Lf/rh6;->get(I)Ljava/lang/Object;

    .line 72
    .line 73
    .line 74
    move-result-object v4

    .line 75
    check-cast v4, Lf/ga8$m37;

    .line 76
    .line 77
    iget-object v5, v4, Lf/ga8$m37;->a61:Ljava/lang/String;

    .line 78
    .line 79
    invoke-virtual {v5, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 80
    .line 81
    .line 82
    move-result v5

    .line 83
    if-nez v5, :cond_55

    .line 84
    .line 85
    goto :goto_59

    .line 86
    :cond_55
    iget v5, v4, Lf/ga8$m37;->Zo1:I

    .line 87
    .line 88
    if-eq v5, p1, :cond_5c

    .line 89
    .line 90
    :goto_59
    add-int/lit8 v3, v3, 0x1

    .line 91
    .line 92
    goto :goto_44

    .line 93
    :cond_5c
    move-object v1, v4

    .line 94
    :cond_5d
    :goto_5d
    if-eqz v1, :cond_60

    .line 95
    .line 96
    return-object v1

    .line 97
    :cond_60
    iget-object p1, p0, Lf/r41;->bE:Lf/m39;

    .line 98
    .line 99
    return-object p1
.end method

.method public final Pl()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->x61:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final RW1()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->tH:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final SN1(IZ)Lf/m39;
    .registers 4

    .line 1
    if-ltz p1, :cond_7

    .line 2
    .line 3
    iget-object v0, p0, Lf/r41;->Nl1:[Lf/m39;

    .line 4
    .line 5
    array-length v0, v0

    .line 6
    if-lt p1, v0, :cond_8

    .line 7
    .line 8
    :cond_7
    const/4 p1, 0x0

    .line 9
    :cond_8
    sget-object v0, Lf/c89;->m10:Lf/c89;

    .line 10
    .line 11
    if-nez p1, :cond_11

    .line 12
    .line 13
    if-eqz p2, :cond_11

    .line 14
    .line 15
    iget-object p1, p0, Lf/r41;->rB1:Lf/m39;

    .line 16
    .line 17
    return-object p1

    .line 18
    :cond_11
    iget-object p2, p0, Lf/r41;->Nl1:[Lf/m39;

    .line 19
    .line 20
    aget-object p1, p2, p1

    .line 21
    .line 22
    return-object p1
.end method

.method public final aa(B)Lf/m39;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/r41;->lt:[Lf/m39;

    .line 2
    .line 3
    invoke-static {p1}, Lf/r41;->lp0(B)I

    .line 4
    .line 5
    .line 6
    move-result p1

    .line 7
    aget-object p1, v0, p1

    .line 8
    .line 9
    return-object p1
.end method

.method public final dispose()V
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->HC1:Lf/ga8;

    .line 2
    .line 3
    if-eqz v0, :cond_7

    .line 4
    .line 5
    invoke-virtual {v0}, Lf/ga8;->dispose()V

    .line 6
    .line 7
    .line 8
    :cond_7
    iget-object v0, p0, Lf/r41;->Rw:Lf/dk2;

    .line 9
    .line 10
    if-eqz v0, :cond_e

    .line 11
    .line 12
    invoke-virtual {v0}, Lf/dk2;->dispose()V

    .line 13
    .line 14
    .line 15
    :cond_e
    iget-object v0, p0, Lf/r41;->eo:Lcom/badlogic/gdx/graphics/Texture;

    .line 16
    .line 17
    invoke-virtual {v0}, Lcom/badlogic/gdx/graphics/Texture;->dispose()V

    .line 18
    .line 19
    .line 20
    iget-object v0, p0, Lf/r41;->Ck1:Lcom/badlogic/gdx/graphics/Texture;

    .line 21
    .line 22
    invoke-virtual {v0}, Lcom/badlogic/gdx/graphics/Texture;->dispose()V

    .line 23
    .line 24
    .line 25
    iget-object v0, p0, Lf/r41;->QZ0:Lcom/badlogic/gdx/graphics/Texture;

    .line 26
    .line 27
    invoke-virtual {v0}, Lcom/badlogic/gdx/graphics/Texture;->dispose()V

    .line 28
    .line 29
    .line 30
    iget-object v0, p0, Lf/r41;->Ic:Lcom/badlogic/gdx/graphics/Texture;

    .line 31
    .line 32
    invoke-virtual {v0}, Lcom/badlogic/gdx/graphics/Texture;->dispose()V

    .line 33
    .line 34
    .line 35
    return-void
.end method

.method public final varargs iZ0([Ljava/lang/String;)Lcom/badlogic/gdx/graphics/Texture;
    .registers 7

    .line 1
    array-length v0, p1

    .line 2
    const/4 v1, 0x0

    .line 3
    :goto_2
    if-ge v1, v0, :cond_2a

    .line 4
    .line 5
    aget-object v2, p1, v1

    .line 6
    .line 7
    const-string v3, "data/sprites/textures/"

    .line 8
    .line 9
    const-string v4, ".png"

    .line 10
    .line 11
    invoke-static {v3, v2, v4}, Lf/cz7;->Ls0(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 12
    .line 13
    .line 14
    move-result-object v2

    .line 15
    sget-object v3, Lf/p37;->K2:Lf/px0;

    .line 16
    .line 17
    invoke-virtual {v3, v2}, Lf/px0;->yZ0(Ljava/lang/String;)Lf/v9;

    .line 18
    .line 19
    .line 20
    move-result-object v3

    .line 21
    invoke-virtual {v3}, Lf/v9;->zz1()Lf/z46;

    .line 22
    .line 23
    .line 24
    move-result-object v3

    .line 25
    if-nez v3, :cond_1b

    .line 26
    .line 27
    goto :goto_27

    .line 28
    :cond_1b
    :try_start_1b
    new-instance v3, Lcom/badlogic/gdx/graphics/Texture;

    .line 29
    .line 30
    sget-object v4, Lf/dq7;->vZ1:Lf/u43;

    .line 31
    .line 32
    invoke-virtual {v4, v2}, Lf/u43;->G4(Ljava/lang/String;)Lf/rz;

    .line 33
    .line 34
    .line 35
    move-result-object v2

    .line 36
    invoke-direct {v3, v2}, Lcom/badlogic/gdx/graphics/Texture;-><init>(Lf/z46;)V
    :try_end_26
    .catch Ljava/lang/Exception; {:try_start_1b .. :try_end_26} :catch_27

    .line 37
    .line 38
    .line 39
    return-object v3

    .line 40
    :catch_27
    :goto_27
    add-int/lit8 v1, v1, 0x1

    .line 41
    .line 42
    goto :goto_2

    .line 43
    :cond_2a
    iget-object p1, p0, Lf/r41;->QZ0:Lcom/badlogic/gdx/graphics/Texture;

    .line 44
    .line 45
    return-object p1
.end method

.method public final qp0(Ljava/lang/String;)Lf/m39;
    .registers 6

    .line 1
    iget-object v0, p0, Lf/r41;->Rw:Lf/dk2;

    .line 2
    .line 3
    const/4 v1, 0x0

    .line 4
    if-eqz v0, :cond_29

    .line 5
    .line 6
    iget-object v2, v0, Lf/dk2;->pc:Lf/z46;

    .line 7
    .line 8
    const-string v3, ".png"

    .line 9
    .line 10
    invoke-virtual {p1, v3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 11
    .line 12
    .line 13
    move-result-object p1

    .line 14
    invoke-virtual {v2, p1}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 15
    .line 16
    .line 17
    move-result-object p1

    .line 18
    invoke-virtual {p1}, Lf/z46;->hz1()Z

    .line 19
    .line 20
    .line 21
    move-result v2

    .line 22
    if-nez v2, :cond_18

    .line 23
    .line 24
    goto :goto_31

    .line 25
    :cond_18
    new-instance v1, Lcom/badlogic/gdx/graphics/Texture;

    .line 26
    .line 27
    invoke-direct {v1, p1}, Lcom/badlogic/gdx/graphics/Texture;-><init>(Lf/z46;)V

    .line 28
    .line 29
    .line 30
    iget-object p1, v0, Lf/dk2;->op:Ljava/util/ArrayList;

    .line 31
    .line 32
    invoke-virtual {p1, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 33
    .line 34
    .line 35
    new-instance p1, Lf/m39;

    .line 36
    .line 37
    invoke-direct {p1, v1}, Lf/m39;-><init>(Lcom/badlogic/gdx/graphics/Texture;)V

    .line 38
    .line 39
    .line 40
    move-object v1, p1

    .line 41
    goto :goto_31

    .line 42
    :cond_29
    iget-object v0, p0, Lf/r41;->HC1:Lf/ga8;

    .line 43
    .line 44
    if-eqz v0, :cond_31

    .line 45
    .line 46
    invoke-virtual {v0, p1}, Lf/ga8;->LpT3(Ljava/lang/String;)Lf/ga8$m37;

    .line 47
    .line 48
    .line 49
    move-result-object v1

    .line 50
    :cond_31
    :goto_31
    if-eqz v1, :cond_34

    .line 51
    .line 52
    return-object v1

    .line 53
    :cond_34
    iget-object p1, p0, Lf/r41;->bE:Lf/m39;

    .line 54
    .line 55
    return-object p1
.end method

.method public final rG1(B)Lf/m39;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/r41;->U60:[Lf/m39;

    .line 2
    .line 3
    invoke-static {p1}, Lf/r41;->lp0(B)I

    .line 4
    .line 5
    .line 6
    move-result p1

    .line 7
    aget-object p1, v0, p1

    .line 8
    .line 9
    return-object p1
.end method

.method public final rN1(IZZ)Lf/m39;
    .registers 5

    .line 1
    if-ltz p1, :cond_7

    .line 2
    .line 3
    iget-object v0, p0, Lf/r41;->ZM:[Lf/m39;

    .line 4
    .line 5
    array-length v0, v0

    .line 6
    if-lt p1, v0, :cond_8

    .line 7
    .line 8
    :cond_7
    const/4 p1, 0x0

    .line 9
    :cond_8
    if-eqz p2, :cond_1a

    .line 10
    .line 11
    if-nez p3, :cond_15

    .line 12
    .line 13
    const/4 p2, 0x4

    .line 14
    if-ne p1, p2, :cond_10

    .line 15
    .line 16
    goto :goto_15

    .line 17
    :cond_10
    iget-object p2, p0, Lf/r41;->K6:[Lf/m39;

    .line 18
    .line 19
    aget-object p1, p2, p1

    .line 20
    .line 21
    return-object p1

    .line 22
    :cond_15
    :goto_15
    iget-object p2, p0, Lf/r41;->lq0:[Lf/m39;

    .line 23
    .line 24
    aget-object p1, p2, p1

    .line 25
    .line 26
    return-object p1

    .line 27
    :cond_1a
    iget-object p2, p0, Lf/r41;->ZM:[Lf/m39;

    .line 28
    .line 29
    aget-object p1, p2, p1

    .line 30
    .line 31
    return-object p1
.end method

.method public final ua1(I)Lf/m39;
    .registers 4

    .line 1
    iget-object v0, p0, Lf/r41;->lh1:[Lf/m39;

    .line 2
    .line 3
    array-length v1, v0

    .line 4
    if-lt p1, v1, :cond_9

    .line 5
    .line 6
    sget-object p1, Lf/eb6;->nw0:Lf/eb6;

    .line 7
    .line 8
    iget-byte p1, p1, Lf/eb6;->qx:B

    .line 9
    .line 10
    :cond_9
    aget-object p1, v0, p1

    .line 11
    .line 12
    return-object p1
.end method

.method public final ub0()Lf/m39;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/r41;->tV1:Lf/m39;

    .line 2
    .line 3
    return-object v0
.end method

.method public final z80(Ljava/lang/String;)V
    .registers 11

    .line 1
    iget-object v0, p0, Lf/r41;->DG1:Ljava/lang/String;

    .line 2
    .line 3
    if-eqz v0, :cond_b

    .line 4
    .line 5
    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 6
    .line 7
    .line 8
    move-result v0

    .line 9
    if-eqz v0, :cond_b

    .line 10
    .line 11
    return-void

    .line 12
    :cond_b
    iput-object p1, p0, Lf/r41;->DG1:Ljava/lang/String;

    .line 13
    .line 14
    sget-object v0, Lf/gt0;->t3:[Ljava/lang/String;

    .line 15
    .line 16
    new-instance v0, Ljava/lang/StringBuilder;

    .line 17
    .line 18
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 19
    .line 20
    .line 21
    const/4 v1, 0x0

    .line 22
    const/4 v2, 0x0

    .line 23
    :goto_16
    invoke-virtual {p1}, Ljava/lang/String;->length()I

    .line 24
    .line 25
    .line 26
    move-result v3

    .line 27
    if-ge v2, v3, :cond_2c

    .line 28
    .line 29
    invoke-virtual {p1, v2}, Ljava/lang/String;->charAt(I)C

    .line 30
    .line 31
    .line 32
    move-result v3

    .line 33
    invoke-static {v3}, Ljava/lang/Character;->isLetter(C)Z

    .line 34
    .line 35
    .line 36
    move-result v4

    .line 37
    if-eqz v4, :cond_29

    .line 38
    .line 39
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    .line 40
    .line 41
    .line 42
    :cond_29
    add-int/lit8 v2, v2, 0x1

    .line 43
    .line 44
    goto :goto_16

    .line 45
    :cond_2c
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 46
    .line 47
    .line 48
    move-result-object p1

    .line 49
    # MonMMO-EX: 20 badge slots (was 18) so type 19, Fairy, loads monster_type_19 / skill_type_19.
    const/16 v0, 0x14

    .line 50
    .line 51
    new-array v2, v0, [Lf/m39;

    .line 52
    .line 53
    const/4 v3, 0x0

    .line 54
    :goto_35
    const-string v4, "_en"

    .line 55
    .line 56
    const-string v5, "_"

    .line 57
    .line 58
    if-ge v3, v0, :cond_71

    .line 59
    .line 60
    new-instance v6, Ljava/lang/StringBuilder;

    .line 61
    .line 62
    const-string v7, "monster_type_"

    .line 63
    .line 64
    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 65
    .line 66
    .line 67
    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 68
    .line 69
    .line 70
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 71
    .line 72
    .line 73
    invoke-virtual {v6, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 74
    .line 75
    .line 76
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 77
    .line 78
    .line 79
    move-result-object v5

    .line 80
    invoke-virtual {p0, v5}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 81
    .line 82
    .line 83
    move-result-object v5

    .line 84
    aput-object v5, v2, v3

    .line 85
    .line 86
    iget-object v6, p0, Lf/r41;->bE:Lf/m39;

    .line 87
    .line 88
    if-ne v5, v6, :cond_6e

    .line 89
    .line 90
    new-instance v5, Ljava/lang/StringBuilder;

    .line 91
    .line 92
    invoke-direct {v5, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 93
    .line 94
    .line 95
    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 96
    .line 97
    .line 98
    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 99
    .line 100
    .line 101
    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 102
    .line 103
    .line 104
    move-result-object v4

    .line 105
    invoke-virtual {p0, v4}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 106
    .line 107
    .line 108
    move-result-object v4

    .line 109
    aput-object v4, v2, v3

    .line 110
    .line 111
    :cond_6e
    add-int/lit8 v3, v3, 0x1

    .line 112
    .line 113
    goto :goto_35

    .line 114
    :cond_71
    iput-object v2, p0, Lf/r41;->lh1:[Lf/m39;

    .line 115
    .line 116
    new-array v2, v0, [Lf/m39;

    .line 117
    .line 118
    const/4 v3, 0x0

    .line 119
    :goto_76
    if-ge v3, v0, :cond_ae

    .line 120
    .line 121
    new-instance v6, Ljava/lang/StringBuilder;

    .line 122
    .line 123
    const-string v7, "skill_type_"

    .line 124
    .line 125
    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 126
    .line 127
    .line 128
    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 129
    .line 130
    .line 131
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 132
    .line 133
    .line 134
    invoke-virtual {v6, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 135
    .line 136
    .line 137
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 138
    .line 139
    .line 140
    move-result-object v6

    .line 141
    invoke-virtual {p0, v6}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 142
    .line 143
    .line 144
    move-result-object v6

    .line 145
    aput-object v6, v2, v3

    .line 146
    .line 147
    iget-object v8, p0, Lf/r41;->bE:Lf/m39;

    .line 148
    .line 149
    if-ne v6, v8, :cond_ab

    .line 150
    .line 151
    new-instance v6, Ljava/lang/StringBuilder;

    .line 152
    .line 153
    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 154
    .line 155
    .line 156
    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 157
    .line 158
    .line 159
    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 160
    .line 161
    .line 162
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 163
    .line 164
    .line 165
    move-result-object v6

    .line 166
    invoke-virtual {p0, v6}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 167
    .line 168
    .line 169
    move-result-object v6

    .line 170
    aput-object v6, v2, v3

    .line 171
    .line 172
    :cond_ab
    add-int/lit8 v3, v3, 0x1

    .line 173
    .line 174
    goto :goto_76

    .line 175
    :cond_ae
    iput-object v2, p0, Lf/r41;->CF:[Lf/m39;

    .line 176
    .line 177
    const/4 v0, 0x3

    .line 178
    new-array v2, v0, [Lf/m39;

    .line 179
    .line 180
    :goto_b3
    if-ge v1, v0, :cond_eb

    .line 181
    .line 182
    new-instance v3, Ljava/lang/StringBuilder;

    .line 183
    .line 184
    const-string v6, "skill_damage_"

    .line 185
    .line 186
    invoke-direct {v3, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 187
    .line 188
    .line 189
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 190
    .line 191
    .line 192
    invoke-virtual {v3, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 193
    .line 194
    .line 195
    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 196
    .line 197
    .line 198
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 199
    .line 200
    .line 201
    move-result-object v3

    .line 202
    invoke-virtual {p0, v3}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 203
    .line 204
    .line 205
    move-result-object v3

    .line 206
    aput-object v3, v2, v1

    .line 207
    .line 208
    iget-object v7, p0, Lf/r41;->bE:Lf/m39;

    .line 209
    .line 210
    if-ne v3, v7, :cond_e8

    .line 211
    .line 212
    new-instance v3, Ljava/lang/StringBuilder;

    .line 213
    .line 214
    invoke-direct {v3, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 215
    .line 216
    .line 217
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 218
    .line 219
    .line 220
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 221
    .line 222
    .line 223
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 224
    .line 225
    .line 226
    move-result-object v3

    .line 227
    invoke-virtual {p0, v3}, Lf/r41;->qp0(Ljava/lang/String;)Lf/m39;

    .line 228
    .line 229
    .line 230
    move-result-object v3

    .line 231
    aput-object v3, v2, v1

    .line 232
    .line 233
    :cond_e8
    add-int/lit8 v1, v1, 0x1

    .line 234
    .line 235
    goto :goto_b3

    .line 236
    :cond_eb
    iput-object v2, p0, Lf/r41;->Iq1:[Lf/m39;

    .line 237
    .line 238
    return-void
.end method
