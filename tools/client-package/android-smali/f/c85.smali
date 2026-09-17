.class public final Lf/c85;
.super Ljava/lang/Object;


# static fields
.field public static final LC1:Ljava/util/List;

.field public static final Sl0:Ljava/util/List;

.field public static final jL:Lf/xv7;

.field public static final zL1:Lf/xv7;


# instance fields
.field public final BP1:Lf/z46;

.field public DD:Ljava/lang/String;

.field public DQ:Lf/z46;

.field public FD:Z

.field public FX0:Z

.field public final Ib:Lf/k89;

.field public final Ky1:Ljava/util/ArrayList;

.field public O2:Ljava/lang/String;

.field public TE:Lf/im1;

.field public final Vb0:Lf/ch4;

.field public final WY1:Ljava/util/ArrayList;

.field public Zl0:Ljava/lang/String;

.field public cOM9:Lf/qj6;

.field public final ek:Ljava/util/ArrayList;

.field public final:Ljava/lang/String;

.field public final hQ0:Ljava/util/ArrayList;

.field public final mx1:Ljava/util/ArrayList;

.field public final qc1:Z

.field public final uP0:Ljava/util/ArrayList;

.field public final wH1:Lf/k89;

.field public xd1:Ljava/util/zip/ZipFile;

.field public final yz:Ljava/util/ArrayList;

.field public zZ1:Ljava/lang/String;


# direct methods
.method static constructor <clinit>()V
    .registers 4

    .line 1
    const-class v0, Lf/c85;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Lf/c85;->zL1:Lf/xv7;

    .line 8
    .line 9
    const-string v0, "mod"

    .line 10
    .line 11
    invoke-static {v0}, Lf/tv7;->xr0(Ljava/lang/String;)Lf/xv7;

    .line 12
    .line 13
    .line 14
    move-result-object v0

    .line 15
    sput-object v0, Lf/c85;->jL:Lf/xv7;

    .line 16
    .line 17
    const-string v0, "data/sprites/textures/"

    .line 18
    .line 19
    const-string v1, "data/sprites/atlas/"

    .line 20
    .line 21
    const-string v2, "data/strings/"

    .line 22
    .line 23
    const-string v3, "data/themes/"

    .line 24
    .line 25
    filled-new-array {v1, v2, v3, v0}, [Ljava/lang/String;

    .line 26
    .line 27
    .line 28
    move-result-object v0

    .line 29
    invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 30
    .line 31
    .line 32
    move-result-object v0

    .line 33
    sput-object v0, Lf/c85;->LC1:Ljava/util/List;

    .line 34
    .line 35
    filled-new-array {v2, v3}, [Ljava/lang/String;

    .line 36
    .line 37
    .line 38
    move-result-object v0

    .line 39
    invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 40
    .line 41
    .line 42
    move-result-object v0

    .line 43
    sput-object v0, Lf/c85;->Sl0:Ljava/util/List;

    .line 44
    .line 45
    return-void
.end method

.method public constructor <init>(Lf/z46;Z)V
    .registers 5

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    const/4 v0, 0x0

    .line 5
    iput-boolean v0, p0, Lf/c85;->FD:Z

    .line 6
    .line 7
    iput-boolean v0, p0, Lf/c85;->FX0:Z

    .line 8
    .line 9
    const/4 v0, 0x0

    .line 10
    iput-object v0, p0, Lf/c85;->xd1:Ljava/util/zip/ZipFile;

    .line 11
    .line 12
    iput-object v0, p0, Lf/c85;->TE:Lf/im1;

    .line 13
    .line 14
    const-string v1, ""

    .line 15
    .line 16
    iput-object v1, p0, Lf/c85;->zZ1:Ljava/lang/String;

    .line 17
    .line 18
    iput-object v1, p0, Lf/c85;->DD:Ljava/lang/String;

    .line 19
    .line 20
    iput-object v1, p0, Lf/c85;->O2:Ljava/lang/String;

    .line 21
    .line 22
    iput-object v1, p0, Lf/c85;->final:Ljava/lang/String;

    .line 23
    .line 24
    iput-object v1, p0, Lf/c85;->Zl0:Ljava/lang/String;

    .line 25
    .line 26
    iput-object v0, p0, Lf/c85;->cOM9:Lf/qj6;

    .line 27
    .line 28
    new-instance v0, Lf/k89;

    .line 29
    .line 30
    invoke-direct {v0}, Lf/x44;-><init>()V

    .line 31
    .line 32
    .line 33
    iput-object v0, p0, Lf/c85;->wH1:Lf/k89;

    .line 34
    .line 35
    new-instance v0, Lf/k89;

    .line 36
    .line 37
    invoke-direct {v0}, Lf/x44;-><init>()V

    .line 38
    .line 39
    .line 40
    iput-object v0, p0, Lf/c85;->Ib:Lf/k89;

    .line 41
    .line 42
    new-instance v0, Lf/ch4;

    .line 43
    .line 44
    invoke-direct {v0}, Lf/pl6;-><init>()V

    .line 45
    .line 46
    .line 47
    iput-object v0, p0, Lf/c85;->Vb0:Lf/ch4;

    .line 48
    .line 49
    new-instance v0, Ljava/util/ArrayList;

    .line 50
    .line 51
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 52
    .line 53
    .line 54
    iput-object v0, p0, Lf/c85;->WY1:Ljava/util/ArrayList;

    .line 55
    .line 56
    new-instance v0, Ljava/util/ArrayList;

    .line 57
    .line 58
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 59
    .line 60
    .line 61
    iput-object v0, p0, Lf/c85;->ek:Ljava/util/ArrayList;

    .line 62
    .line 63
    new-instance v0, Ljava/util/ArrayList;

    .line 64
    .line 65
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 66
    .line 67
    .line 68
    iput-object v0, p0, Lf/c85;->hQ0:Ljava/util/ArrayList;

    .line 69
    .line 70
    new-instance v0, Ljava/util/ArrayList;

    .line 71
    .line 72
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 73
    .line 74
    .line 75
    iput-object v0, p0, Lf/c85;->mx1:Ljava/util/ArrayList;

    .line 76
    .line 77
    new-instance v0, Ljava/util/ArrayList;

    .line 78
    .line 79
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 80
    .line 81
    .line 82
    iput-object v0, p0, Lf/c85;->uP0:Ljava/util/ArrayList;

    .line 83
    .line 84
    new-instance v0, Ljava/util/ArrayList;

    .line 85
    .line 86
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 87
    .line 88
    .line 89
    iput-object v0, p0, Lf/c85;->yz:Ljava/util/ArrayList;

    .line 90
    .line 91
    new-instance v0, Ljava/util/ArrayList;

    .line 92
    .line 93
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 94
    .line 95
    .line 96
    iput-object v0, p0, Lf/c85;->Ky1:Ljava/util/ArrayList;

    .line 97
    .line 98
    iput-object p1, p0, Lf/c85;->BP1:Lf/z46;

    .line 99
    .line 100
    iput-boolean p2, p0, Lf/c85;->qc1:Z

    .line 101
    .line 102
    return-void
.end method

.method public static QH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Lorg/w3c/dom/Element;
    .registers 4

    .line 1
    invoke-interface {p0, p1}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 2
    .line 3
    .line 4
    move-result-object v0

    .line 5
    invoke-interface {v0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 6
    .line 7
    .line 8
    move-result v1

    .line 9
    if-nez v1, :cond_13

    .line 10
    .line 11
    invoke-interface {p0}, Lorg/w3c/dom/Node;->getOwnerDocument()Lorg/w3c/dom/Document;

    .line 12
    .line 13
    .line 14
    move-result-object p0

    .line 15
    invoke-interface {p0, p1}, Lorg/w3c/dom/Document;->createElement(Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 16
    .line 17
    .line 18
    move-result-object p0

    .line 19
    return-object p0

    .line 20
    :cond_13
    const/4 p0, 0x0

    .line 21
    invoke-interface {v0, p0}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 22
    .line 23
    .line 24
    move-result-object p0

    .line 25
    check-cast p0, Lorg/w3c/dom/Element;

    .line 26
    .line 27
    return-object p0
.end method

.method public static TH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Ljava/util/ArrayList;
    .registers 16

    .line 1
    new-instance v0, Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 4
    .line 5
    .line 6
    const-string v1, "constants"

    .line 7
    .line 8
    invoke-static {p0, v1}, Lf/c85;->QH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 9
    .line 10
    .line 11
    move-result-object p0

    .line 12
    const-string v1, "constant"

    .line 13
    .line 14
    invoke-interface {p0, v1}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 15
    .line 16
    .line 17
    move-result-object p0

    .line 18
    const/4 v1, 0x0

    .line 19
    const/4 v2, 0x0

    .line 20
    :goto_13
    invoke-interface {p0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 21
    .line 22
    .line 23
    move-result v3

    .line 24
    if-ge v2, v3, :cond_9a

    .line 25
    .line 26
    invoke-interface {p0, v2}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 27
    .line 28
    .line 29
    move-result-object v3

    .line 30
    check-cast v3, Lorg/w3c/dom/Element;

    .line 31
    .line 32
    const-string v4, "name"

    .line 33
    .line 34
    invoke-interface {v3, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 35
    .line 36
    .line 37
    move-result-object v6

    .line 38
    const-string v4, "type"

    .line 39
    .line 40
    invoke-interface {v3, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 41
    .line 42
    .line 43
    move-result-object v4

    .line 44
    invoke-virtual {v4}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    .line 45
    .line 46
    .line 47
    move-result-object v4

    .line 48
    const-string v5, "default_value"

    .line 49
    .line 50
    invoke-interface {v3, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 51
    .line 52
    .line 53
    move-result-object v10

    .line 54
    const-string v5, "public_name"

    .line 55
    .line 56
    invoke-interface {v3, v5}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 57
    .line 58
    .line 59
    move-result v7

    .line 60
    if-eqz v7, :cond_43

    .line 61
    .line 62
    invoke-interface {v3, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 63
    .line 64
    .line 65
    move-result-object v7

    .line 66
    move-object v8, v7

    .line 67
    goto :goto_44

    .line 68
    :cond_43
    move-object v8, v6

    .line 69
    :goto_44
    const-string v7, "whitelist_value"

    .line 70
    .line 71
    invoke-interface {v3, v7}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 72
    .line 73
    .line 74
    move-result-object v3

    .line 75
    new-instance v7, Ljava/util/ArrayList;

    .line 76
    .line 77
    invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V

    .line 78
    .line 79
    .line 80
    const/4 v9, 0x0

    .line 81
    :goto_50
    invoke-interface {v3}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 82
    .line 83
    .line 84
    move-result v11

    .line 85
    if-ge v9, v11, :cond_77

    .line 86
    .line 87
    invoke-interface {v3, v9}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 88
    .line 89
    .line 90
    move-result-object v11

    .line 91
    check-cast v11, Lorg/w3c/dom/Element;

    .line 92
    .line 93
    invoke-interface {v11}, Lorg/w3c/dom/Node;->getTextContent()Ljava/lang/String;

    .line 94
    .line 95
    .line 96
    move-result-object v12

    .line 97
    invoke-interface {v11, v5}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 98
    .line 99
    .line 100
    move-result v13

    .line 101
    if-eqz v13, :cond_6b

    .line 102
    .line 103
    invoke-interface {v11, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 104
    .line 105
    .line 106
    move-result-object v11

    .line 107
    goto :goto_6c

    .line 108
    :cond_6b
    move-object v11, v12

    .line 109
    :goto_6c
    new-instance v13, Lf/eu4;

    .line 110
    .line 111
    invoke-direct {v13, v11, v12}, Lf/eu4;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    .line 112
    .line 113
    .line 114
    invoke-virtual {v7, v13}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 115
    .line 116
    .line 117
    add-int/lit8 v9, v9, 0x1

    .line 118
    .line 119
    goto :goto_50

    .line 120
    :cond_77
    invoke-virtual {v7}, Ljava/util/ArrayList;->isEmpty()Z

    .line 121
    .line 122
    .line 123
    move-result v3

    .line 124
    if-nez v3, :cond_87

    .line 125
    .line 126
    new-array v3, v1, [Lf/eu4;

    .line 127
    .line 128
    invoke-virtual {v7, v3}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 129
    .line 130
    .line 131
    move-result-object v3

    .line 132
    check-cast v3, [Lf/eu4;

    .line 133
    .line 134
    :goto_85
    move-object v11, v3

    .line 135
    goto :goto_89

    .line 136
    :cond_87
    const/4 v3, 0x0

    .line 137
    goto :goto_85

    .line 138
    :goto_89
    new-instance v5, Lf/wh4;

    .line 139
    .line 140
    invoke-static {v4}, Lf/vt3;->valueOf(Ljava/lang/String;)Lf/vt3;

    .line 141
    .line 142
    .line 143
    move-result-object v9

    .line 144
    move-object v7, p1

    .line 145
    invoke-direct/range {v5 .. v11}, Lf/wh4;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lf/vt3;Ljava/lang/String;[Lf/eu4;)V

    .line 146
    .line 147
    .line 148
    invoke-virtual {v0, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 149
    .line 150
    .line 151
    add-int/lit8 v2, v2, 0x1

    .line 152
    .line 153
    goto/16 :goto_13

    .line 154
    .line 155
    :cond_9a
    return-object v0
.end method

.method public static g70(Lf/z46;B)V
    .registers 10

    .line 1
    const/4 v0, 0x0

    .line 2
    invoke-virtual {p0, v0}, Lf/z46;->xN1(Ljava/lang/String;)Ljava/lang/String;

    .line 3
    .line 4
    .line 5
    move-result-object p0

    .line 6
    const-string v0, "\n"

    .line 7
    .line 8
    invoke-virtual {p0, v0}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 9
    .line 10
    .line 11
    move-result-object p0

    .line 12
    array-length v0, p0

    .line 13
    const/4 v1, 0x0

    .line 14
    const/4 v2, 0x0

    .line 15
    :goto_e
    if-ge v2, v0, :cond_73

    .line 16
    .line 17
    aget-object v3, p0, v2

    .line 18
    .line 19
    const-string v4, ";"

    .line 20
    .line 21
    invoke-virtual {v3, v4}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 22
    .line 23
    .line 24
    move-result v4

    .line 25
    if-eqz v4, :cond_24

    .line 26
    .line 27
    const/16 v4, 0x3b

    .line 28
    .line 29
    invoke-virtual {v3, v4}, Ljava/lang/String;->indexOf(I)I

    .line 30
    .line 31
    .line 32
    move-result v4

    .line 33
    invoke-virtual {v3, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    .line 34
    .line 35
    .line 36
    move-result-object v3

    .line 37
    :cond_24
    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object v3

    .line 41
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 42
    .line 43
    .line 44
    move-result v4

    .line 45
    if-nez v4, :cond_70

    .line 46
    .line 47
    const-string v4, "="

    .line 48
    .line 49
    invoke-virtual {v3, v4}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 50
    .line 51
    .line 52
    move-result v5

    .line 53
    if-nez v5, :cond_37

    .line 54
    .line 55
    goto :goto_70

    .line 56
    :cond_37
    invoke-virtual {v3, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 57
    .line 58
    .line 59
    move-result-object v3

    .line 60
    array-length v4, v3

    .line 61
    const/4 v5, 0x2

    .line 62
    const-string v6, "Invalid battle sprites scales table value."

    .line 63
    .line 64
    sget-object v7, Lf/c85;->jL:Lf/xv7;

    .line 65
    .line 66
    if-ge v4, v5, :cond_47

    .line 67
    .line 68
    :catch_43
    invoke-interface {v7, v6}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 69
    .line 70
    .line 71
    goto :goto_70

    .line 72
    :cond_47
    :try_start_47
    aget-object v4, v3, v1

    .line 73
    .line 74
    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 75
    .line 76
    .line 77
    move-result-object v4

    .line 78
    invoke-static {v4}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    .line 79
    .line 80
    .line 81
    move-result v4

    .line 82
    const/4 v5, 0x1

    .line 83
    aget-object v3, v3, v5

    .line 84
    .line 85
    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 86
    .line 87
    .line 88
    move-result-object v3

    .line 89
    invoke-static {v3}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    .line 90
    .line 91
    .line 92
    move-result v3
    :try_end_5c
    .catch Ljava/lang/NumberFormatException; {:try_start_47 .. :try_end_5c} :catch_43

    .line 93
    sget-object v5, Lf/pr;->Sb1:Lf/pr;

    .line 94
    .line 95
    iget-object v5, v5, Lf/pr;->wg1:[Lf/ph7;

    .line 96
    .line 97
    aget-object v6, v5, p1

    .line 98
    .line 99
    if-nez v6, :cond_6b

    .line 100
    .line 101
    new-instance v6, Lf/ph7;

    .line 102
    .line 103
    invoke-direct {v6}, Lf/ph7;-><init>()V

    .line 104
    .line 105
    .line 106
    aput-object v6, v5, p1

    .line 107
    .line 108
    :cond_6b
    aget-object v5, v5, p1

    .line 109
    .line 110
    invoke-virtual {v5, v4, v3}, Lf/ph7;->pl1(SF)V

    .line 111
    .line 112
    .line 113
    :cond_70
    :goto_70
    add-int/lit8 v2, v2, 0x1

    .line 114
    .line 115
    goto :goto_e

    .line 116
    :cond_73
    return-void
.end method

.method public static gD(Lf/z46;)V
    .registers 20

    .line 1
    const/4 v0, 0x0

    .line 2
    move-object/from16 v1, p0

    .line 3
    .line 4
    invoke-virtual {v1, v0}, Lf/z46;->xN1(Ljava/lang/String;)Ljava/lang/String;

    .line 5
    .line 6
    .line 7
    move-result-object v0

    .line 8
    const-string v1, "\n"

    .line 9
    .line 10
    invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 11
    .line 12
    .line 13
    move-result-object v0

    .line 14
    array-length v1, v0

    .line 15
    const/4 v2, 0x0

    .line 16
    const/4 v3, 0x0

    .line 17
    :goto_10
    if-ge v3, v1, :cond_cc

    .line 18
    .line 19
    aget-object v4, v0, v3

    .line 20
    .line 21
    const-string v5, ";"

    .line 22
    .line 23
    invoke-virtual {v4, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 24
    .line 25
    .line 26
    move-result v5

    .line 27
    if-eqz v5, :cond_26

    .line 28
    .line 29
    const/16 v5, 0x3b

    .line 30
    .line 31
    invoke-virtual {v4, v5}, Ljava/lang/String;->indexOf(I)I

    .line 32
    .line 33
    .line 34
    move-result v5

    .line 35
    invoke-virtual {v4, v5}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    .line 36
    .line 37
    .line 38
    move-result-object v4

    .line 39
    :cond_26
    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 40
    .line 41
    .line 42
    move-result-object v4

    .line 43
    invoke-virtual {v4}, Ljava/lang/String;->isEmpty()Z

    .line 44
    .line 45
    .line 46
    move-result v5

    .line 47
    if-nez v5, :cond_c8

    .line 48
    .line 49
    const-string v5, "="

    .line 50
    .line 51
    invoke-virtual {v4, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 52
    .line 53
    .line 54
    move-result v6

    .line 55
    if-nez v6, :cond_3a

    .line 56
    .line 57
    goto/16 :goto_c8

    .line 58
    .line 59
    :cond_3a
    invoke-virtual {v4, v5}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 60
    .line 61
    .line 62
    move-result-object v5

    .line 63
    array-length v6, v5

    .line 64
    const-string v7, "Invalid battle sprite altitude table value. {}"

    .line 65
    .line 66
    const/4 v8, 0x2

    .line 67
    sget-object v9, Lf/c85;->jL:Lf/xv7;

    .line 68
    .line 69
    if-ge v6, v8, :cond_4b

    .line 70
    .line 71
    :catch_46
    :goto_46
    invoke-interface {v9, v7, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 72
    .line 73
    .line 74
    goto/16 :goto_c8

    .line 75
    .line 76
    :cond_4b
    aget-object v6, v5, v2

    .line 77
    .line 78
    const-string v10, ","

    .line 79
    .line 80
    invoke-virtual {v6, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 81
    .line 82
    .line 83
    move-result-object v6

    .line 84
    array-length v11, v6

    .line 85
    if-ge v11, v8, :cond_57

    .line 86
    .line 87
    goto :goto_46

    .line 88
    :cond_57
    const/4 v11, 0x1

    .line 89
    aget-object v5, v5, v11

    .line 90
    .line 91
    invoke-virtual {v5, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 92
    .line 93
    .line 94
    move-result-object v5

    .line 95
    array-length v10, v5

    .line 96
    const/4 v12, 0x3

    .line 97
    if-eq v10, v12, :cond_68

    .line 98
    .line 99
    const-string v5, "Invalid battle sprite altitude table value. Coordinates must be defined as X,Y,Z {}"

    .line 100
    .line 101
    invoke-interface {v9, v5, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 102
    .line 103
    .line 104
    goto :goto_c8

    .line 105
    :cond_68
    new-instance v10, Lf/u07;

    .line 106
    .line 107
    invoke-direct {v10}, Lf/u07;-><init>()V

    .line 108
    .line 109
    .line 110
    :try_start_6d
    aget-object v12, v6, v2

    .line 111
    .line 112
    invoke-virtual {v12}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 113
    .line 114
    .line 115
    move-result-object v12

    .line 116
    invoke-static {v12}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    .line 117
    .line 118
    .line 119
    move-result v13

    .line 120
    aget-object v6, v6, v11

    .line 121
    .line 122
    invoke-virtual {v6}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 123
    .line 124
    .line 125
    move-result-object v6

    .line 126
    const-string v12, "back"

    .line 127
    .line 128
    invoke-virtual {v6, v12}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 129
    .line 130
    .line 131
    move-result v14

    .line 132
    aget-object v6, v5, v2

    .line 133
    .line 134
    invoke-static {v6}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    .line 135
    .line 136
    .line 137
    move-result v6

    .line 138
    const/high16 v12, 0x3f800000    # 1.0f

    .line 139
    .line 140
    const/high16 v15, -0x40800000    # -1.0f

    .line 141
    .line 142
    invoke-static {v6, v15, v12}, Lf/vx1;->ij(FFF)F

    .line 143
    .line 144
    .line 145
    move-result v6

    .line 146
    iput v6, v10, Lf/u07;->x:F

    .line 147
    .line 148
    aget-object v6, v5, v11

    .line 149
    .line 150
    invoke-static {v6}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    .line 151
    .line 152
    .line 153
    move-result v6

    .line 154
    invoke-static {v6, v15, v12}, Lf/vx1;->ij(FFF)F

    .line 155
    .line 156
    .line 157
    move-result v6

    .line 158
    iput v6, v10, Lf/u07;->y:F

    .line 159
    .line 160
    aget-object v5, v5, v8

    .line 161
    .line 162
    invoke-static {v5}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    .line 163
    .line 164
    .line 165
    move-result v5

    .line 166
    invoke-static {v5, v15, v12}, Lf/vx1;->ij(FFF)F

    .line 167
    .line 168
    .line 169
    move-result v5

    .line 170
    iput v5, v10, Lf/u07;->z:F
    :try_end_ab
    .catch Ljava/lang/NumberFormatException; {:try_start_6d .. :try_end_ab} :catch_46

    .line 171
    .line 172
    sget-object v4, Lf/pr;->Sb1:Lf/pr;

    .line 173
    .line 174
    iget-object v5, v4, Lf/pr;->F31:Lf/k89;

    .line 175
    .line 176
    if-nez v5, :cond_b8

    .line 177
    .line 178
    new-instance v5, Lf/k89;

    .line 179
    .line 180
    invoke-direct {v5}, Lf/x44;-><init>()V

    .line 181
    .line 182
    .line 183
    iput-object v5, v4, Lf/pr;->F31:Lf/k89;

    .line 184
    .line 185
    :cond_b8
    iget-object v4, v4, Lf/pr;->F31:Lf/k89;

    .line 186
    .line 187
    const/16 v17, 0x0

    .line 188
    .line 189
    const/16 v18, 0x0

    .line 190
    .line 191
    const/4 v15, 0x0

    .line 192
    const/16 v16, 0x0

    .line 193
    .line 194
    invoke-static/range {v13 .. v18}, Lf/pr;->TO1(SZZBZZ)I

    .line 195
    .line 196
    .line 197
    move-result v5

    .line 198
    invoke-virtual {v4, v5, v10}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    .line 199
    .line 200
    .line 201
    :cond_c8
    :goto_c8
    add-int/lit8 v3, v3, 0x1

    .line 202
    .line 203
    goto/16 :goto_10

    .line 204
    .line 205
    :cond_cc
    return-void
.end method


# virtual methods
.method public final D(Lorg/w3c/dom/Element;)Z
    .registers 12

    .line 1
    const-string v0, "strings"

    .line 2
    .line 3
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 4
    .line 5
    .line 6
    move-result-object p1

    .line 7
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    sget-object v1, Lf/c85;->jL:Lf/xv7;

    .line 12
    .line 13
    const/4 v2, 0x0

    .line 14
    const/4 v3, 0x1

    .line 15
    if-le v0, v3, :cond_16

    .line 16
    .line 17
    const-string p1, "Mods are only allowed to define on \'strings\' section"

    .line 18
    .line 19
    invoke-interface {v1, p1}, Lf/xv7;->error(Ljava/lang/String;)V

    .line 20
    .line 21
    .line 22
    return v2

    .line 23
    :cond_16
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 24
    .line 25
    .line 26
    move-result v0

    .line 27
    if-ne v0, v3, :cond_c6

    .line 28
    .line 29
    invoke-interface {p1, v2}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 30
    .line 31
    .line 32
    move-result-object p1

    .line 33
    check-cast p1, Lorg/w3c/dom/Element;

    .line 34
    .line 35
    const-string v0, "string"

    .line 36
    .line 37
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 38
    .line 39
    .line 40
    move-result-object v0

    .line 41
    const/4 v4, 0x0

    .line 42
    :goto_29
    invoke-interface {v0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 43
    .line 44
    .line 45
    move-result v5

    .line 46
    if-ge v4, v5, :cond_c6

    .line 47
    .line 48
    invoke-interface {v0, v4}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 49
    .line 50
    .line 51
    move-result-object v5

    .line 52
    check-cast v5, Lorg/w3c/dom/Element;

    .line 53
    .line 54
    const-string v6, "string_revision"

    .line 55
    .line 56
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 57
    .line 58
    .line 59
    move-result v7

    .line 60
    const-string v8, "revision"

    .line 61
    .line 62
    if-eqz v7, :cond_46

    .line 63
    .line 64
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 65
    .line 66
    .line 67
    move-result-object v6

    .line 68
    invoke-interface {v5, v8, v6}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 69
    .line 70
    .line 71
    :cond_46
    const-string v6, "path"

    .line 72
    .line 73
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 74
    .line 75
    .line 76
    move-result v7

    .line 77
    iget-object v9, p0, Lf/c85;->BP1:Lf/z46;

    .line 78
    .line 79
    if-nez v7, :cond_5c

    .line 80
    .line 81
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 82
    .line 83
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 84
    .line 85
    .line 86
    move-result-object p1

    .line 87
    const-string v0, "String has no path attribute: {}"

    .line 88
    .line 89
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 90
    .line 91
    .line 92
    return v2

    .line 93
    :cond_5c
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 94
    .line 95
    .line 96
    move-result-object v6

    .line 97
    const-string v7, ".xml"

    .line 98
    .line 99
    invoke-virtual {v6, v7}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    .line 100
    .line 101
    .line 102
    move-result v6

    .line 103
    if-nez v6, :cond_73

    .line 104
    .line 105
    iget-object v6, v9, Lf/z46;->O01:Ljava/io/File;

    .line 106
    .line 107
    invoke-virtual {v6}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 108
    .line 109
    .line 110
    move-result-object v6

    .line 111
    const-string v7, "String path does not point to xml file: {}"

    .line 112
    .line 113
    invoke-interface {v1, v7, v6}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 114
    .line 115
    .line 116
    :cond_73
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 117
    .line 118
    .line 119
    move-result v6

    .line 120
    if-nez v6, :cond_85

    .line 121
    .line 122
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 123
    .line 124
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 125
    .line 126
    .line 127
    move-result-object p1

    .line 128
    const-string v0, "String has no revision attribute: {}"

    .line 129
    .line 130
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 131
    .line 132
    .line 133
    return v2

    .line 134
    :cond_85
    :try_start_85
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 135
    .line 136
    .line 137
    move-result-object v6

    .line 138
    invoke-static {v6}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 139
    .line 140
    .line 141
    move-result v6

    .line 142
    if-le v6, v3, :cond_ad

    .line 143
    .line 144
    const-string p1, "String revision {} is above current revision {}: {}"

    .line 145
    .line 146
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 147
    .line 148
    .line 149
    move-result-object v0

    .line 150
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 151
    .line 152
    .line 153
    move-result-object v4

    .line 154
    iget-object v6, v9, Lf/z46;->O01:Ljava/io/File;

    .line 155
    .line 156
    invoke-virtual {v6}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 157
    .line 158
    .line 159
    move-result-object v6

    .line 160
    const/4 v7, 0x3

    .line 161
    new-array v7, v7, [Ljava/lang/Object;

    .line 162
    .line 163
    aput-object v0, v7, v2

    .line 164
    .line 165
    aput-object v4, v7, v3

    .line 166
    .line 167
    const/4 v0, 0x2

    .line 168
    aput-object v6, v7, v0

    .line 169
    .line 170
    invoke-interface {v1, p1, v7}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V
    :try_end_ac
    .catch Ljava/lang/NumberFormatException; {:try_start_85 .. :try_end_ac} :catch_b6

    .line 171
    .line 172
    .line 173
    return v2

    .line 174
    :cond_ad
    iget-object v6, p0, Lf/c85;->hQ0:Ljava/util/ArrayList;

    .line 175
    .line 176
    invoke-virtual {v6, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 177
    .line 178
    .line 179
    add-int/lit8 v4, v4, 0x1

    .line 180
    .line 181
    goto/16 :goto_29

    .line 182
    .line 183
    :catch_b6
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 184
    .line 185
    .line 186
    move-result-object p1

    .line 187
    iget-object v0, v9, Lf/z46;->O01:Ljava/io/File;

    .line 188
    .line 189
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 190
    .line 191
    .line 192
    move-result-object v0

    .line 193
    const-string v3, "String revision {} is not a number: {}"

    .line 194
    .line 195
    invoke-interface {v1, v3, p1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 196
    .line 197
    .line 198
    return v2

    .line 199
    :cond_c6
    return v3
.end method

.method public final RQ0()Z
    .registers 14

    .line 1
    iget-object v0, p0, Lf/c85;->uP0:Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    const/4 v2, 0x0

    .line 8
    const/4 v3, 0x0

    .line 9
    :goto_8
    if-ge v3, v1, :cond_9d

    .line 10
    .line 11
    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 12
    .line 13
    .line 14
    move-result-object v4

    .line 15
    add-int/lit8 v3, v3, 0x1

    .line 16
    .line 17
    check-cast v4, Lorg/w3c/dom/Element;

    .line 18
    .line 19
    const-string v5, "path"

    .line 20
    .line 21
    invoke-interface {v4, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 22
    .line 23
    .line 24
    move-result-object v5

    .line 25
    iget-object v6, p0, Lf/c85;->DQ:Lf/z46;

    .line 26
    .line 27
    invoke-virtual {v6, v5}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 28
    .line 29
    .line 30
    move-result-object v6

    .line 31
    invoke-virtual {v6}, Lf/z46;->hz1()Z

    .line 32
    .line 33
    .line 34
    move-result v6

    .line 35
    iget-object v7, p0, Lf/c85;->BP1:Lf/z46;

    .line 36
    .line 37
    sget-object v8, Lf/c85;->jL:Lf/xv7;

    .line 38
    .line 39
    if-nez v6, :cond_34

    .line 40
    .line 41
    iget-object v0, v7, Lf/z46;->O01:Ljava/io/File;

    .line 42
    .line 43
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 44
    .line 45
    .line 46
    move-result-object v0

    .line 47
    const-string v1, "Path {} does not exist in mod {}"

    .line 48
    .line 49
    :goto_30
    invoke-interface {v8, v1, v5, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 50
    .line 51
    .line 52
    return v2

    .line 53
    :cond_34
    const-string v6, "name"

    .line 54
    .line 55
    invoke-interface {v4, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 56
    .line 57
    .line 58
    move-result-object v6

    .line 59
    const-string v9, "is_mobile"

    .line 60
    .line 61
    invoke-interface {v4, v9}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 62
    .line 63
    .line 64
    move-result-object v9

    .line 65
    invoke-static {v9}, Ljava/lang/Boolean;->parseBoolean(Ljava/lang/String;)Z

    .line 66
    .line 67
    .line 68
    move-result v9

    .line 69
    new-instance v10, Ljava/lang/StringBuilder;

    .line 70
    .line 71
    const-string v11, "extension-"

    .line 72
    .line 73
    invoke-direct {v10, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 74
    .line 75
    .line 76
    invoke-virtual {v10, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 77
    .line 78
    .line 79
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 80
    .line 81
    .line 82
    move-result-object v6

    .line 83
    invoke-static {v4, v6}, Lf/c85;->TH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Ljava/util/ArrayList;

    .line 84
    .line 85
    .line 86
    move-result-object v6

    .line 87
    new-instance v10, Lf/px0;

    .line 88
    .line 89
    invoke-direct {v10}, Lf/px0;-><init>()V

    .line 90
    .line 91
    .line 92
    invoke-virtual {v10}, Lf/px0;->BE()V

    .line 93
    .line 94
    .line 95
    iget-object v11, p0, Lf/c85;->DQ:Lf/z46;

    .line 96
    .line 97
    iget-object v12, v10, Lf/px0;->m9:Ljava/util/List;

    .line 98
    .line 99
    invoke-interface {v12, v2, v11}, Ljava/util/List;->add(ILjava/lang/Object;)V

    .line 100
    .line 101
    .line 102
    new-instance v11, Lf/rr;

    .line 103
    .line 104
    invoke-virtual {v10, v5}, Lf/px0;->yZ0(Ljava/lang/String;)Lf/v9;

    .line 105
    .line 106
    .line 107
    move-result-object v10

    .line 108
    const-string v12, "revision"

    .line 109
    .line 110
    invoke-interface {v4, v12}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 111
    .line 112
    .line 113
    move-result-object v4

    .line 114
    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 115
    .line 116
    .line 117
    move-result v4

    .line 118
    invoke-direct {v11}, Ljava/lang/Object;-><init>()V

    .line 119
    .line 120
    .line 121
    iput-object v10, v11, Lf/rr;->Sx:Lf/v9;

    .line 122
    .line 123
    iput-boolean v9, v11, Lf/rr;->nz1:Z

    .line 124
    .line 125
    iput-object v6, v11, Lf/rr;->Mm1:Ljava/util/ArrayList;

    .line 126
    .line 127
    iput v4, v11, Lf/rr;->cOM1:I

    .line 128
    .line 129
    invoke-virtual {v10}, Lf/v9;->mw0()Z

    .line 130
    .line 131
    .line 132
    move-result v4

    .line 133
    if-eqz v4, :cond_87

    .line 134
    .line 135
    goto :goto_8d

    .line 136
    :cond_87
    invoke-virtual {v10}, Lf/v9;->hz1()Z

    .line 137
    .line 138
    .line 139
    move-result v4

    .line 140
    if-nez v4, :cond_96

    .line 141
    .line 142
    :goto_8d
    iget-object v0, v7, Lf/z46;->O01:Ljava/io/File;

    .line 143
    .line 144
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 145
    .line 146
    .line 147
    move-result-object v0

    .line 148
    const-string v1, "Theme-Extension with path {} in mod {} is not valid"

    .line 149
    .line 150
    goto :goto_30

    .line 151
    :cond_96
    iget-object v4, p0, Lf/c85;->yz:Ljava/util/ArrayList;

    .line 152
    .line 153
    invoke-virtual {v4, v11}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 154
    .line 155
    .line 156
    goto/16 :goto_8

    .line 157
    .line 158
    :cond_9d
    const/4 v0, 0x1

    .line 159
    return v0
.end method

.method public final SG()Z
    .registers 10

    .line 1
    iget-object v0, p0, Lf/c85;->Ky1:Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    const/4 v2, 0x0

    .line 8
    const/4 v3, 0x0

    .line 9
    :cond_8
    if-ge v3, v1, :cond_3d

    .line 10
    .line 11
    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 12
    .line 13
    .line 14
    move-result-object v4

    .line 15
    add-int/lit8 v3, v3, 0x1

    .line 16
    .line 17
    check-cast v4, Ljava/lang/String;

    .line 18
    .line 19
    iget-object v5, p0, Lf/c85;->DQ:Lf/z46;

    .line 20
    .line 21
    invoke-virtual {v5, v4}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 22
    .line 23
    .line 24
    move-result-object v5

    .line 25
    invoke-virtual {v5}, Lf/z46;->hz1()Z

    .line 26
    .line 27
    .line 28
    move-result v6

    .line 29
    iget-object v7, p0, Lf/c85;->BP1:Lf/z46;

    .line 30
    .line 31
    sget-object v8, Lf/c85;->jL:Lf/xv7;

    .line 32
    .line 33
    if-nez v6, :cond_2e

    .line 34
    .line 35
    iget-object v0, v7, Lf/z46;->O01:Ljava/io/File;

    .line 36
    .line 37
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object v0

    .line 41
    const-string v1, "Path {} does not exist in mod {}"

    .line 42
    .line 43
    :goto_2a
    invoke-interface {v8, v1, v4, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 44
    .line 45
    .line 46
    return v2

    .line 47
    :cond_2e
    invoke-virtual {v5}, Lf/z46;->mw0()Z

    .line 48
    .line 49
    .line 50
    move-result v5

    .line 51
    if-nez v5, :cond_8

    .line 52
    .line 53
    iget-object v0, v7, Lf/z46;->O01:Ljava/io/File;

    .line 54
    .line 55
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 56
    .line 57
    .line 58
    move-result-object v0

    .line 59
    const-string v1, "Path {} is not a directory in mod {}"

    .line 60
    .line 61
    goto :goto_2a

    .line 62
    :cond_3d
    const/4 v0, 0x1

    .line 63
    return v0
.end method

.method public final V10()V
    .registers 9

    .line 1
    sget-object v0, Lf/c85;->LC1:Ljava/util/List;

    .line 2
    .line 3
    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    :cond_6
    :goto_6
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    .line 8
    .line 9
    .line 10
    move-result v1

    .line 11
    iget-object v2, p0, Lf/c85;->Ky1:Ljava/util/ArrayList;

    .line 12
    .line 13
    if-eqz v1, :cond_36

    .line 14
    .line 15
    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16
    .line 17
    .line 18
    move-result-object v1

    .line 19
    check-cast v1, Ljava/lang/String;

    .line 20
    .line 21
    invoke-virtual {v2, v1}, Ljava/util/ArrayList;->contains(Ljava/lang/Object;)Z

    .line 22
    .line 23
    .line 24
    move-result v3

    .line 25
    if-eqz v3, :cond_1b

    .line 26
    .line 27
    goto :goto_6

    .line 28
    :cond_1b
    iget-object v3, p0, Lf/c85;->DQ:Lf/z46;

    .line 29
    .line 30
    invoke-virtual {v3, v1}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 31
    .line 32
    .line 33
    move-result-object v3

    .line 34
    invoke-virtual {v3}, Lf/z46;->hz1()Z

    .line 35
    .line 36
    .line 37
    move-result v3

    .line 38
    if-eqz v3, :cond_6

    .line 39
    .line 40
    sget-object v3, Lf/ms5;->KU1:Ljava/util/List;

    .line 41
    .line 42
    new-instance v4, Lf/us8;

    .line 43
    .line 44
    const/4 v5, 0x1

    .line 45
    invoke-direct {v4, p0, v1, v5}, Lf/us8;-><init>(Ljava/lang/Object;Ljava/io/Serializable;I)V

    .line 46
    .line 47
    .line 48
    invoke-interface {v3, v4}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 49
    .line 50
    .line 51
    invoke-virtual {v2, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 52
    .line 53
    .line 54
    goto :goto_6

    .line 55
    :cond_36
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 56
    .line 57
    .line 58
    move-result v0

    .line 59
    const/4 v1, 0x0

    .line 60
    const/4 v3, 0x0

    .line 61
    :cond_3c
    :goto_3c
    if-ge v3, v0, :cond_5a

    .line 62
    .line 63
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 64
    .line 65
    .line 66
    move-result-object v4

    .line 67
    add-int/lit8 v3, v3, 0x1

    .line 68
    .line 69
    check-cast v4, Ljava/lang/String;

    .line 70
    .line 71
    sget-object v5, Lf/c85;->Sl0:Ljava/util/List;

    .line 72
    .line 73
    invoke-interface {v5, v4}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    .line 74
    .line 75
    .line 76
    move-result v5

    .line 77
    if-eqz v5, :cond_3c

    .line 78
    .line 79
    sget-object v5, Lf/ms5;->KU1:Ljava/util/List;

    .line 80
    .line 81
    new-instance v6, Lf/us8;

    .line 82
    .line 83
    const/4 v7, 0x2

    .line 84
    invoke-direct {v6, p0, v4, v7}, Lf/us8;-><init>(Ljava/lang/Object;Ljava/io/Serializable;I)V

    .line 85
    .line 86
    .line 87
    invoke-interface {v5, v6}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 88
    .line 89
    .line 90
    goto :goto_3c

    .line 91
    :cond_5a
    sget-object v0, Lf/gf5;->xL1:Lf/gf5;

    .line 92
    .line 93
    iget-object v0, v0, Lf/gf5;->rV:Ljava/util/ArrayList;

    .line 94
    .line 95
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    .line 96
    .line 97
    .line 98
    move-result v2

    .line 99
    :cond_62
    :goto_62
    if-ge v1, v2, :cond_9b

    .line 100
    .line 101
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 102
    .line 103
    .line 104
    move-result-object v3

    .line 105
    add-int/lit8 v1, v1, 0x1

    .line 106
    .line 107
    check-cast v3, Lf/c85;

    .line 108
    .line 109
    iget-object v4, v3, Lf/c85;->Ky1:Ljava/util/ArrayList;

    .line 110
    .line 111
    invoke-static {v4}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 112
    .line 113
    .line 114
    move-result-object v4

    .line 115
    new-instance v5, Lf/mn;

    .line 116
    .line 117
    const/16 v6, 0x17

    .line 118
    .line 119
    invoke-direct {v5, v6, p0}, Lf/mn;-><init>(ILjava/lang/Object;)V

    .line 120
    .line 121
    .line 122
    invoke-interface {v4, v5}, Lj$/util/stream/Stream;->filter(Ljava/util/function/Predicate;)Lj$/util/stream/Stream;

    .line 123
    .line 124
    .line 125
    move-result-object v4

    .line 126
    invoke-static {}, Lj$/util/stream/Collectors;->toSet()Lj$/util/stream/Collector;

    .line 127
    .line 128
    .line 129
    move-result-object v5

    .line 130
    invoke-interface {v4, v5}, Lj$/util/stream/Stream;->collect(Lj$/util/stream/Collector;)Ljava/lang/Object;

    .line 131
    .line 132
    .line 133
    move-result-object v4

    .line 134
    check-cast v4, Ljava/util/Set;

    .line 135
    .line 136
    invoke-interface {v4}, Ljava/util/Set;->isEmpty()Z

    .line 137
    .line 138
    .line 139
    move-result v5

    .line 140
    if-nez v5, :cond_62

    .line 141
    .line 142
    invoke-static {v4}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 143
    .line 144
    .line 145
    move-result-object v4

    .line 146
    new-instance v5, Lf/se5;

    .line 147
    .line 148
    const/4 v6, 0x4

    .line 149
    invoke-direct {v5, p0, v6, v3}, Lf/se5;-><init>(Ljava/lang/Object;ILjava/lang/Object;)V

    .line 150
    .line 151
    .line 152
    invoke-interface {v4, v5}, Lj$/util/stream/Stream;->forEach(Ljava/util/function/Consumer;)V

    .line 153
    .line 154
    .line 155
    goto :goto_62

    .line 156
    :cond_9b
    return-void
.end method

.method public final VA(Lorg/w3c/dom/Element;)Z
    .registers 9

    .line 1
    const-string v0, "overlays"

    .line 2
    .line 3
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 4
    .line 5
    .line 6
    move-result-object p1

    .line 7
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    sget-object v1, Lf/c85;->jL:Lf/xv7;

    .line 12
    .line 13
    const/4 v2, 0x0

    .line 14
    const/4 v3, 0x1

    .line 15
    if-le v0, v3, :cond_16

    .line 16
    .line 17
    const-string p1, "Mods are only allowed to define one \'overlays\' section"

    .line 18
    .line 19
    invoke-interface {v1, p1}, Lf/xv7;->error(Ljava/lang/String;)V

    .line 20
    .line 21
    .line 22
    return v2

    .line 23
    :cond_16
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 24
    .line 25
    .line 26
    move-result v0

    .line 27
    if-ne v0, v3, :cond_63

    .line 28
    .line 29
    invoke-interface {p1, v2}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 30
    .line 31
    .line 32
    move-result-object p1

    .line 33
    check-cast p1, Lorg/w3c/dom/Element;

    .line 34
    .line 35
    const-string v0, "overlay"

    .line 36
    .line 37
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 38
    .line 39
    .line 40
    move-result-object p1

    .line 41
    const/4 v0, 0x0

    .line 42
    :goto_29
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 43
    .line 44
    .line 45
    move-result v4

    .line 46
    if-ge v0, v4, :cond_63

    .line 47
    .line 48
    invoke-interface {p1, v0}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 49
    .line 50
    .line 51
    move-result-object v4

    .line 52
    check-cast v4, Lorg/w3c/dom/Element;

    .line 53
    .line 54
    const-string v5, "path"

    .line 55
    .line 56
    invoke-interface {v4, v5}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 57
    .line 58
    .line 59
    move-result v6

    .line 60
    if-nez v6, :cond_4b

    .line 61
    .line 62
    iget-object p1, p0, Lf/c85;->BP1:Lf/z46;

    .line 63
    .line 64
    iget-object p1, p1, Lf/z46;->O01:Ljava/io/File;

    .line 65
    .line 66
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 67
    .line 68
    .line 69
    move-result-object p1

    .line 70
    const-string v0, "Overlay has no path attribute: {}"

    .line 71
    .line 72
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 73
    .line 74
    .line 75
    return v2

    .line 76
    :cond_4b
    invoke-interface {v4, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 77
    .line 78
    .line 79
    move-result-object v4

    .line 80
    const-string v5, "/"

    .line 81
    .line 82
    invoke-virtual {v4, v5}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    .line 83
    .line 84
    .line 85
    move-result v6

    .line 86
    if-nez v6, :cond_5b

    .line 87
    .line 88
    invoke-virtual {v4, v5}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 89
    .line 90
    .line 91
    move-result-object v4

    .line 92
    :cond_5b
    iget-object v5, p0, Lf/c85;->Ky1:Ljava/util/ArrayList;

    .line 93
    .line 94
    invoke-virtual {v5, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 95
    .line 96
    .line 97
    add-int/lit8 v0, v0, 0x1

    .line 98
    .line 99
    goto :goto_29

    .line 100
    :cond_63
    return v3
.end method

.method public final VW()Z
    .registers 10

    .line 1
    const/4 v0, 0x0

    .line 2
    :try_start_1
    invoke-virtual {p0}, Lf/c85;->YM0()Z

    .line 3
    .line 4
    .line 5
    move-result v1

    .line 6
    if-nez v1, :cond_8

    .line 7
    .line 8
    return v0

    .line 9
    :cond_8
    iget-object v1, p0, Lf/c85;->yz:Ljava/util/ArrayList;

    .line 10
    .line 11
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 12
    .line 13
    .line 14
    move-result v2

    .line 15
    const/4 v3, 0x0

    .line 16
    :goto_f
    const/4 v4, 0x1

    .line 17
    if-ge v3, v2, :cond_30

    .line 18
    .line 19
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 20
    .line 21
    .line 22
    move-result-object v5

    .line 23
    add-int/lit8 v3, v3, 0x1

    .line 24
    .line 25
    check-cast v5, Lf/rr;

    .line 26
    .line 27
    iget v6, v5, Lf/rr;->cOM1:I

    .line 28
    .line 29
    if-le v4, v6, :cond_2a

    .line 30
    .line 31
    sget-object v4, Lf/ms5;->KU1:Ljava/util/List;

    .line 32
    .line 33
    new-instance v6, Lf/ba;

    .line 34
    .line 35
    const/4 v7, 0x5

    .line 36
    invoke-direct {v6, v7, v5}, Lf/ba;-><init>(ILjava/lang/Object;)V

    .line 37
    .line 38
    .line 39
    invoke-interface {v4, v6}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 40
    .line 41
    .line 42
    goto :goto_f

    .line 43
    :cond_2a
    sget-object v4, Lf/ms5;->fY0:Ljava/util/List;

    .line 44
    .line 45
    invoke-interface {v4, v5}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 46
    .line 47
    .line 48
    goto :goto_f

    .line 49
    :cond_30
    iget-object v1, p0, Lf/c85;->mx1:Ljava/util/ArrayList;

    .line 50
    .line 51
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 52
    .line 53
    .line 54
    move-result v2

    .line 55
    const/4 v3, 0x0

    .line 56
    :goto_37
    if-ge v3, v2, :cond_5c

    .line 57
    .line 58
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 59
    .line 60
    .line 61
    move-result-object v5

    .line 62
    add-int/lit8 v3, v3, 0x1

    .line 63
    .line 64
    check-cast v5, Lf/g40;

    .line 65
    .line 66
    iget v6, v5, Lf/g40;->uQ:I

    .line 67
    .line 68
    if-le v4, v6, :cond_47

    .line 69
    .line 70
    const/4 v6, 0x1

    .line 71
    goto :goto_48

    .line 72
    :cond_47
    const/4 v6, 0x0

    .line 73
    :goto_48
    if-eqz v6, :cond_56

    .line 74
    .line 75
    sget-object v6, Lf/ms5;->KU1:Ljava/util/List;

    .line 76
    .line 77
    new-instance v7, Lf/ba;

    .line 78
    .line 79
    const/4 v8, 0x6

    .line 80
    invoke-direct {v7, v8, v5}, Lf/ba;-><init>(ILjava/lang/Object;)V

    .line 81
    .line 82
    .line 83
    invoke-interface {v6, v7}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 84
    .line 85
    .line 86
    goto :goto_37

    .line 87
    :cond_56
    sget-object v6, Lf/mh2;->ge1:Lf/mh2;

    .line 88
    .line 89
    invoke-virtual {v6, v5}, Lf/mh2;->FV0(Lf/g40;)V

    .line 90
    .line 91
    .line 92
    goto :goto_37

    .line 93
    :cond_5c
    invoke-virtual {p0}, Lf/c85;->V10()V

    .line 94
    .line 95
    .line 96
    iget-object v1, p0, Lf/c85;->xd1:Ljava/util/zip/ZipFile;

    .line 97
    .line 98
    if-eqz v1, :cond_83

    .line 99
    .line 100
    invoke-virtual {v1}, Ljava/util/zip/ZipFile;->entries()Ljava/util/Enumeration;

    .line 101
    .line 102
    .line 103
    move-result-object v1

    .line 104
    :goto_67
    invoke-interface {v1}, Ljava/util/Enumeration;->hasMoreElements()Z

    .line 105
    .line 106
    .line 107
    move-result v2

    .line 108
    if-eqz v2, :cond_88

    .line 109
    .line 110
    invoke-interface {v1}, Ljava/util/Enumeration;->nextElement()Ljava/lang/Object;

    .line 111
    .line 112
    .line 113
    move-result-object v2

    .line 114
    check-cast v2, Ljava/util/zip/ZipEntry;

    .line 115
    .line 116
    iget-object v3, p0, Lf/c85;->DQ:Lf/z46;

    .line 117
    .line 118
    invoke-virtual {v2}, Ljava/util/zip/ZipEntry;->getName()Ljava/lang/String;

    .line 119
    .line 120
    .line 121
    move-result-object v2

    .line 122
    invoke-virtual {v3, v2}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 123
    .line 124
    .line 125
    move-result-object v2

    .line 126
    invoke-virtual {p0, v2}, Lf/c85;->ZY1(Lf/z46;)V

    .line 127
    .line 128
    .line 129
    goto :goto_67

    .line 130
    :catch_81
    move-exception v1

    .line 131
    goto :goto_ae

    .line 132
    :cond_83
    iget-object v1, p0, Lf/c85;->DQ:Lf/z46;

    .line 133
    .line 134
    invoke-virtual {p0, v1}, Lf/c85;->Yw1(Lf/z46;)V

    .line 135
    .line 136
    .line 137
    :cond_88
    iget-object v1, p0, Lf/c85;->wH1:Lf/k89;

    .line 138
    .line 139
    new-instance v2, Lf/j57;

    .line 140
    .line 141
    invoke-direct {v2, p0, v0}, Lf/j57;-><init>(Lf/c85;I)V

    .line 142
    .line 143
    .line 144
    invoke-virtual {v1, v2}, Lf/k89;->Az(Lf/j57;)Z

    .line 145
    .line 146
    .line 147
    iget-object v2, p0, Lf/c85;->Ib:Lf/k89;

    .line 148
    .line 149
    new-instance v3, Lf/j57;

    .line 150
    .line 151
    invoke-direct {v3, p0, v4}, Lf/j57;-><init>(Lf/c85;I)V

    .line 152
    .line 153
    .line 154
    invoke-virtual {v2, v3}, Lf/k89;->Az(Lf/j57;)Z

    .line 155
    .line 156
    .line 157
    invoke-virtual {v1}, Lf/k89;->clear()V

    .line 158
    .line 159
    .line 160
    iget-object v1, p0, Lf/c85;->Vb0:Lf/ch4;

    .line 161
    .line 162
    invoke-virtual {v1}, Lf/ch4;->clear()V

    .line 163
    .line 164
    .line 165
    invoke-virtual {v2}, Lf/k89;->clear()V

    .line 166
    .line 167
    .line 168
    iget-boolean v1, p0, Lf/c85;->FX0:Z

    .line 169
    .line 170
    if-eqz v1, :cond_ad

    .line 171
    .line 172
    iput-boolean v4, p0, Lf/c85;->FD:Z
    :try_end_ad
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_ad} :catch_81

    .line 173
    .line 174
    :cond_ad
    return v4

    .line 175
    :goto_ae
    iget-object v2, p0, Lf/c85;->BP1:Lf/z46;

    .line 176
    .line 177
    iget-object v3, v2, Lf/z46;->O01:Ljava/io/File;

    .line 178
    .line 179
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 180
    .line 181
    .line 182
    move-result-object v3

    .line 183
    sget-object v4, Lf/c85;->zL1:Lf/xv7;

    .line 184
    .line 185
    const-string v5, "Error applying mod {}"

    .line 186
    .line 187
    invoke-interface {v4, v5, v3, v1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 188
    .line 189
    .line 190
    iget-object v2, v2, Lf/z46;->O01:Ljava/io/File;

    .line 191
    .line 192
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 193
    .line 194
    .line 195
    move-result-object v2

    .line 196
    sget-object v3, Lf/c85;->jL:Lf/xv7;

    .line 197
    .line 198
    invoke-interface {v3, v5, v2, v1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 199
    .line 200
    .line 201
    return v0
.end method

.method public final YM0()Z
    .registers 9

    .line 1
    iget-object v0, p0, Lf/c85;->ek:Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-static {v0}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 4
    .line 5
    .line 6
    move-result-object v1

    .line 7
    new-instance v2, Lf/oc7;

    .line 8
    .line 9
    const/16 v3, 0x1c

    .line 10
    .line 11
    invoke-direct {v2, v3}, Lf/oc7;-><init>(I)V

    .line 12
    .line 13
    .line 14
    invoke-interface {v1, v2}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 15
    .line 16
    .line 17
    move-result-object v1

    .line 18
    new-instance v2, Lf/bj0;

    .line 19
    .line 20
    const/4 v3, 0x4

    .line 21
    invoke-direct {v2, v3}, Lf/bj0;-><init>(I)V

    .line 22
    .line 23
    .line 24
    invoke-interface {v1, v2}, Lj$/util/stream/Stream;->filter(Ljava/util/function/Predicate;)Lj$/util/stream/Stream;

    .line 25
    .line 26
    .line 27
    move-result-object v1

    .line 28
    invoke-static {}, Lj$/util/stream/Collectors;->toList()Lj$/util/stream/Collector;

    .line 29
    .line 30
    .line 31
    move-result-object v2

    .line 32
    invoke-interface {v1, v2}, Lj$/util/stream/Stream;->collect(Lj$/util/stream/Collector;)Ljava/lang/Object;

    .line 33
    .line 34
    .line 35
    move-result-object v1

    .line 36
    check-cast v1, Ljava/util/List;

    .line 37
    .line 38
    invoke-interface {v1}, Ljava/util/List;->isEmpty()Z

    .line 39
    .line 40
    .line 41
    move-result v2

    .line 42
    const/4 v3, 0x0

    .line 43
    if-nez v2, :cond_3b

    .line 44
    .line 45
    invoke-static {v1}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 46
    .line 47
    .line 48
    move-result-object v0

    .line 49
    new-instance v1, Lf/zq2;

    .line 50
    .line 51
    const/16 v2, 0xf

    .line 52
    .line 53
    invoke-direct {v1, v2, p0}, Lf/zq2;-><init>(ILjava/lang/Object;)V

    .line 54
    .line 55
    .line 56
    invoke-interface {v0, v1}, Lj$/util/stream/Stream;->forEach(Ljava/util/function/Consumer;)V

    .line 57
    .line 58
    .line 59
    return v3

    .line 60
    :cond_3b
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    .line 61
    .line 62
    .line 63
    move-result v1

    .line 64
    const/4 v2, 0x0

    .line 65
    :goto_40
    const/4 v4, 0x1

    .line 66
    if-ge v2, v1, :cond_77

    .line 67
    .line 68
    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 69
    .line 70
    .line 71
    move-result-object v5

    .line 72
    add-int/lit8 v2, v2, 0x1

    .line 73
    .line 74
    check-cast v5, Lf/xw5;

    .line 75
    .line 76
    invoke-virtual {v5}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 77
    .line 78
    .line 79
    invoke-static {}, Lf/p37;->N91()V

    .line 80
    .line 81
    .line 82
    iget-boolean v6, v5, Lf/xw5;->z50:Z

    .line 83
    .line 84
    if-nez v6, :cond_60

    .line 85
    .line 86
    sget-object v4, Lf/ms5;->KU1:Ljava/util/List;

    .line 87
    .line 88
    new-instance v6, Lf/b60;

    .line 89
    .line 90
    invoke-direct {v6, v5, v3}, Lf/b60;-><init>(Lf/xw5;I)V

    .line 91
    .line 92
    .line 93
    invoke-interface {v4, v6}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 94
    .line 95
    .line 96
    goto :goto_40

    .line 97
    :cond_60
    const/16 v6, 0x8

    .line 98
    .line 99
    iget v7, v5, Lf/xw5;->rH0:I

    .line 100
    .line 101
    if-le v6, v7, :cond_71

    .line 102
    .line 103
    sget-object v6, Lf/ms5;->KU1:Ljava/util/List;

    .line 104
    .line 105
    new-instance v7, Lf/b60;

    .line 106
    .line 107
    invoke-direct {v7, v5, v4}, Lf/b60;-><init>(Lf/xw5;I)V

    .line 108
    .line 109
    .line 110
    invoke-interface {v6, v7}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 111
    .line 112
    .line 113
    goto :goto_40

    .line 114
    :cond_71
    sget-object v4, Lf/ms5;->BK:Ljava/util/List;

    .line 115
    .line 116
    invoke-interface {v4, v5}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 117
    .line 118
    .line 119
    goto :goto_40

    .line 120
    :cond_77
    return v4
.end method

.method public final Yw1(Lf/z46;)V
    .registers 7

    .line 1
    invoke-virtual {p1}, Lf/z46;->A8()[Lf/z46;

    .line 2
    .line 3
    .line 4
    move-result-object p1

    .line 5
    array-length v0, p1

    .line 6
    const/4 v1, 0x0

    .line 7
    :goto_6
    if-ge v1, v0, :cond_28

    .line 8
    .line 9
    aget-object v2, p1, v1

    .line 10
    .line 11
    iget-object v3, v2, Lf/z46;->O01:Ljava/io/File;

    .line 12
    .line 13
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object v3

    .line 17
    const-string v4, "."

    .line 18
    .line 19
    invoke-virtual {v3, v4}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 20
    .line 21
    .line 22
    move-result v3

    .line 23
    if-nez v3, :cond_22

    .line 24
    .line 25
    invoke-virtual {v2}, Lf/z46;->mw0()Z

    .line 26
    .line 27
    .line 28
    move-result v3

    .line 29
    if-eqz v3, :cond_22

    .line 30
    .line 31
    invoke-virtual {p0, v2}, Lf/c85;->Yw1(Lf/z46;)V

    .line 32
    .line 33
    .line 34
    goto :goto_25

    .line 35
    :cond_22
    invoke-virtual {p0, v2}, Lf/c85;->ZY1(Lf/z46;)V

    .line 36
    .line 37
    .line 38
    :goto_25
    add-int/lit8 v1, v1, 0x1

    .line 39
    .line 40
    goto :goto_6

    .line 41
    :cond_28
    return-void
.end method

.method public final ZY1(Lf/z46;)V
    .registers 50

    move-object/from16 v1, p0

    move-object/from16 v0, p1

    const-string v2, "Invalid file: trainersprites/{}/{} has an invalid region ID. Valid region IDs are: 0 / 1 / 2 / 3 / 10"

    const-string v3, "Invalid file: overworldsprites/{}/{} has an invalid region ID. Valid region IDs are: 0 / 1 / 2 / 3 / 10"

    iget-object v4, v1, Lf/c85;->Ib:Lf/k89;

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v5

    sget-object v6, Ljava/util/Locale;->ENGLISH:Ljava/util/Locale;

    invoke-virtual {v5, v6}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v5

    iget-object v7, v1, Lf/c85;->xd1:Ljava/util/zip/ZipFile;

    iget-object v8, v1, Lf/c85;->BP1:Lf/z46;

    const/4 v9, 0x1

    if-nez v7, :cond_37

    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/String;->length()I

    move-result v7

    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v10

    const-string v11, "/"

    invoke-virtual {v10, v11}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v10

    xor-int/2addr v10, v9

    add-int/2addr v7, v10

    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v10

    invoke-virtual {v5, v7, v10}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v5

    :cond_37
    const-string v7, "\\\\|/"

    invoke-virtual {v5, v7}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v5

    array-length v7, v5

    const-string v11, ".png"

    const-string v12, "sprites"

    const-string v13, "Loaded {} from {}"

    const/16 v14, 0x2e

    const/4 v15, 0x3

    const/16 v17, 0x2

    const/4 v10, 0x0

    const/16 v18, 0x1

    sget-object v9, Lf/c85;->jL:Lf/xv7;

    if-ne v7, v15, :cond_cc

    aget-object v7, v5, v10

    invoke-virtual {v12, v7}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v7

    if-eqz v7, :cond_cc

    const-string v7, "itemicons"

    aget-object v15, v5, v18

    invoke-virtual {v7, v15}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v7

    if-eqz v7, :cond_cc

    aget-object v7, v5, v17

    invoke-virtual {v7, v6}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v7, v11}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v15

    if-nez v15, :cond_74

    const-string v0, "Only .png files supported for /sprites/itemicons/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_74
    :try_start_74
    invoke-virtual {v7, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v15

    invoke-virtual {v7, v10, v15}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v15

    invoke-static {v15}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v7
    :try_end_80
    .catch Ljava/lang/NumberFormatException; {:try_start_74 .. :try_end_80} :catch_c6

    sget-boolean v15, Lf/ms5;->T20:Z

    if-eqz v15, :cond_8f

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v15

    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v14

    invoke-interface {v9, v13, v15, v14}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 1
    :cond_8f
    sget-object v14, Lf/c21;->WT1:Lf/c21;

    .line 2
    new-instance v15, Lf/tx8;

    invoke-direct {v15, v0}, Lf/tx8;-><init>(Lf/z46;)V

    invoke-virtual {v14}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-object/from16 v20, v8

    .line 3
    new-instance v8, Lf/qj6;

    move-object/from16 v21, v2

    new-instance v2, Lf/yy0;

    invoke-direct {v2, v15, v10}, Lf/yy0;-><init>(Lf/tx8;I)V

    invoke-direct {v8, v2}, Lf/qj6;-><init>(Lf/ll1;)V

    iget-object v2, v14, Lf/c21;->vi1:Lf/ch4;

    invoke-virtual {v2, v7, v8}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    new-instance v2, Lf/qj6;

    const/16 v22, 0x0

    new-instance v10, Lf/yy0;

    move-object/from16 v23, v8

    const/4 v8, 0x1

    invoke-direct {v10, v15, v8}, Lf/yy0;-><init>(Lf/tx8;I)V

    invoke-direct {v2, v10}, Lf/qj6;-><init>(Lf/ll1;)V

    iget-object v8, v14, Lf/c21;->Pi:Lf/ch4;

    invoke-virtual {v8, v7, v2}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    const/4 v2, -0x1

    if-ne v7, v2, :cond_d2

    sput-object v23, Lf/c21;->UL0:Lf/qj6;

    goto :goto_d2

    .line 4
    :catch_c6
    const-string v0, "{} has an invalid item id."

    invoke-interface {v9, v0, v7}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_cc
    move-object/from16 v21, v2

    move-object/from16 v20, v8

    const/16 v22, 0x0

    :cond_d2
    :goto_d2
    array-length v2, v5

    const-string v7, ";"

    const-string v8, "\n"

    const-string v10, ","

    const-string v14, "="

    const-string v15, "{} has an invalid monster id."

    move-object/from16 v23, v3

    const-string v3, "s"

    move-object/from16 v25, v4

    const-string v4, "f"

    const-string v1, "-"

    move-object/from16 v27, v5

    const/4 v5, 0x3

    if-ne v2, v5, :cond_3bb

    aget-object v2, v27, v22

    invoke-virtual {v12, v2}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_3bb

    const-string v2, "followsprites"

    const/16 v18, 0x1

    aget-object v5, v27, v18

    invoke-virtual {v2, v5}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_3bb

    aget-object v2, v27, v17

    invoke-virtual {v2, v6}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v2

    const-string v5, "atlasdata.txt"

    invoke-virtual {v5, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_2e9

    const/4 v5, 0x0

    .line 5
    invoke-virtual {v0, v5}, Lf/z46;->xN1(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 6
    invoke-virtual {v0, v8}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    array-length v1, v0

    const/4 v2, 0x0

    const/4 v3, 0x0

    :goto_11a
    if-ge v2, v1, :cond_2e5

    aget-object v4, v0, v2

    invoke-virtual {v4, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_126

    goto/16 :goto_2df

    :cond_126
    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/String;->isEmpty()Z

    move-result v5

    if-nez v5, :cond_2df

    invoke-virtual {v4, v14}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v5

    if-nez v5, :cond_138

    goto/16 :goto_2df

    :cond_138
    invoke-virtual {v4, v14}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v5, v4

    const-string v6, "Invalid battle sprites scales table value."

    const/4 v8, 0x2

    if-ge v5, v8, :cond_147

    :catch_142
    invoke-interface {v9, v6}, Lf/xv7;->info(Ljava/lang/String;)V

    goto/16 :goto_2df

    :cond_147
    aget-object v5, v4, v22

    invoke-virtual {v5}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v5

    const-string v8, "columns"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    const-string v11, "Invalid follow sprites table value at line {}"

    const/16 v12, 0x8

    if-eqz v8, :cond_179

    const/16 v18, 0x1

    :try_start_15b
    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v4
    :try_end_165
    .catch Ljava/lang/NumberFormatException; {:try_start_15b .. :try_end_165} :catch_170

    .line 7
    sget-object v5, Lf/im7;->Wr0:Lf/im7;

    .line 8
    invoke-static {v12, v4}, Ljava/lang/Math;->min(II)I

    move-result v4

    int-to-byte v4, v4

    .line 9
    iput-byte v4, v5, Lf/im7;->lR0:B

    goto/16 :goto_2dd

    .line 10
    :catch_170
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    goto/16 :goto_2df

    :cond_179
    const-string v8, "rows"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_198

    const/16 v18, 0x1

    :try_start_183
    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v4
    :try_end_18d
    .catch Ljava/lang/NumberFormatException; {:try_start_183 .. :try_end_18d} :catch_170

    .line 11
    sget-object v5, Lf/im7;->Wr0:Lf/im7;

    .line 12
    invoke-static {v12, v4}, Ljava/lang/Math;->min(II)I

    move-result v4

    int-to-byte v4, v4

    .line 13
    iput-byte v4, v5, Lf/im7;->pm:B

    goto/16 :goto_2dd

    .line 14
    :cond_198
    const-string v8, "north"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_1d8

    const/16 v18, 0x1

    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v5, v4

    const-string v6, "Invalid follow sprite north value at line {}"

    const/4 v8, 0x2

    if-ge v5, v8, :cond_1bb

    :catch_1b2
    :goto_1b2
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-interface {v9, v6, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    goto/16 :goto_2df

    :cond_1bb
    array-length v5, v4

    new-array v8, v5, [B

    const/4 v11, 0x0

    :goto_1bf
    if-ge v11, v5, :cond_1d0

    :try_start_1c1
    aget-object v12, v4, v11

    invoke-virtual {v12}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v12

    invoke-static {v12}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v12

    aput-byte v12, v8, v11
    :try_end_1cd
    .catch Ljava/lang/NumberFormatException; {:try_start_1c1 .. :try_end_1cd} :catch_1b2

    add-int/lit8 v11, v11, 0x1

    goto :goto_1bf

    .line 15
    :cond_1d0
    sget-object v4, Lf/im7;->Wr0:Lf/im7;

    .line 16
    iget-object v4, v4, Lf/im7;->hS1:[[B

    .line 17
    aput-object v8, v4, v22

    goto/16 :goto_2dd

    .line 18
    :cond_1d8
    const-string v8, "south"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    const/16 v18, 0x1

    if-eqz v8, :cond_212

    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v5, v4

    const-string v6, "Invalid follow sprite south value at line {}"

    const/4 v8, 0x2

    if-ge v5, v8, :cond_1f3

    goto :goto_1b2

    :cond_1f3
    array-length v5, v4

    new-array v8, v5, [B

    const/4 v11, 0x0

    :goto_1f7
    if-ge v11, v5, :cond_208

    :try_start_1f9
    aget-object v12, v4, v11

    invoke-virtual {v12}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v12

    invoke-static {v12}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v12

    aput-byte v12, v8, v11
    :try_end_205
    .catch Ljava/lang/NumberFormatException; {:try_start_1f9 .. :try_end_205} :catch_1b2

    add-int/lit8 v11, v11, 0x1

    goto :goto_1f7

    .line 19
    :cond_208
    sget-object v4, Lf/im7;->Wr0:Lf/im7;

    .line 20
    iget-object v4, v4, Lf/im7;->hS1:[[B

    const/16 v18, 0x1

    .line 21
    aput-object v8, v4, v18

    goto/16 :goto_2dd

    .line 22
    :cond_212
    const-string v8, "west"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_249

    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v5, v4

    const-string v6, "Invalid follow sprite west value at line {}"

    const/4 v8, 0x2

    if-ge v5, v8, :cond_22b

    goto :goto_1b2

    :cond_22b
    array-length v5, v4

    new-array v8, v5, [B

    const/4 v11, 0x0

    :goto_22f
    if-ge v11, v5, :cond_240

    :try_start_231
    aget-object v12, v4, v11

    invoke-virtual {v12}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v12

    invoke-static {v12}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v12

    aput-byte v12, v8, v11
    :try_end_23d
    .catch Ljava/lang/NumberFormatException; {:try_start_231 .. :try_end_23d} :catch_1b2

    add-int/lit8 v11, v11, 0x1

    goto :goto_22f

    .line 23
    :cond_240
    sget-object v4, Lf/im7;->Wr0:Lf/im7;

    .line 24
    iget-object v4, v4, Lf/im7;->hS1:[[B

    const/4 v11, 0x2

    .line 25
    aput-object v8, v4, v11

    goto/16 :goto_2dd

    :cond_249
    const/4 v11, 0x2

    .line 26
    const-string v8, "east"

    invoke-virtual {v5, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v5

    const/16 v18, 0x1

    if-eqz v5, :cond_283

    aget-object v4, v4, v18

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    array-length v5, v4

    const-string v6, "Invalid follow sprite east value at line {}"

    if-ge v5, v11, :cond_265

    goto/16 :goto_1b2

    :cond_265
    array-length v5, v4

    new-array v8, v5, [B

    const/4 v11, 0x0

    :goto_269
    if-ge v11, v5, :cond_27a

    :try_start_26b
    aget-object v12, v4, v11

    invoke-virtual {v12}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v12

    invoke-static {v12}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v12

    aput-byte v12, v8, v11
    :try_end_277
    .catch Ljava/lang/NumberFormatException; {:try_start_26b .. :try_end_277} :catch_1b2

    add-int/lit8 v11, v11, 0x1

    goto :goto_269

    .line 27
    :cond_27a
    sget-object v4, Lf/im7;->Wr0:Lf/im7;

    .line 28
    iget-object v4, v4, Lf/im7;->hS1:[[B

    const/16 v19, 0x3

    .line 29
    aput-object v8, v4, v19

    goto :goto_2dd

    .line 30
    :cond_283
    aget-object v5, v4, v18

    invoke-virtual {v5}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v5, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v5

    const/4 v8, 0x4

    new-array v11, v8, [F

    :try_start_290
    aget-object v4, v4, v22

    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v4

    aget-object v8, v5, v22

    invoke-virtual {v8}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v8

    invoke-static {v8}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    move-result v8

    aput v8, v11, v22

    array-length v8, v5

    const/4 v12, 0x1

    if-le v8, v12, :cond_2b6

    aget-object v8, v5, v12

    invoke-virtual {v8}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v8

    invoke-static {v8}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    move-result v8

    aput v8, v11, v12

    :cond_2b6
    array-length v8, v5

    const/4 v12, 0x2

    if-le v8, v12, :cond_2c6

    aget-object v8, v5, v12

    invoke-virtual {v8}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v8

    invoke-static {v8}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    move-result v8

    aput v8, v11, v12

    :cond_2c6
    array-length v8, v5

    const/4 v12, 0x3

    if-le v8, v12, :cond_2d6

    aget-object v5, v5, v12

    invoke-virtual {v5}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v5

    invoke-static {v5}, Ljava/lang/Float;->parseFloat(Ljava/lang/String;)F

    move-result v5

    aput v5, v11, v12
    :try_end_2d6
    .catch Ljava/lang/NumberFormatException; {:try_start_290 .. :try_end_2d6} :catch_142

    .line 31
    :cond_2d6
    sget-object v5, Lf/im7;->Wr0:Lf/im7;

    .line 32
    iget-object v5, v5, Lf/im7;->TS1:Lf/ch4;

    .line 33
    invoke-virtual {v5, v4, v11}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    :goto_2dd
    add-int/lit8 v3, v3, 0x1

    :cond_2df
    :goto_2df
    add-int/lit8 v2, v2, 0x1

    const/16 v17, 0x2

    goto/16 :goto_11a

    :cond_2e5
    move-object/from16 v15, p0

    goto/16 :goto_dc3

    .line 34
    :cond_2e9
    invoke-virtual {v2, v11}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v5

    if-nez v5, :cond_2f5

    const-string v0, "Only .png files supported for /sprites/followsprites/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_2f5
    move-object/from16 v28, v10

    const/16 v5, 0x2e

    invoke-virtual {v2, v5}, Ljava/lang/String;->indexOf(I)I

    move-result v10

    const/4 v5, 0x0

    invoke-virtual {v2, v5, v10}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v10

    const/16 v22, 0x0

    array-length v5, v10

    move-object/from16 v29, v14

    const/4 v14, 0x3

    if-ge v5, v14, :cond_314

    const-string v0, "{} does not have enough fields. Expected name format is ID-X-Y-Z.png where X is \'m\' male or \'f\' female or \'b\' both | Y is \'s\' shiny or \'n\' normal. | Z(optional) is form_id"

    invoke-interface {v9, v0, v2}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_314
    :try_start_314
    aget-object v5, v10, v22

    invoke-static {v5}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v5
    :try_end_31a
    .catch Ljava/lang/NumberFormatException; {:try_start_314 .. :try_end_31a} :catch_3b7

    const/16 v19, 0x3

    array-length v14, v10

    move/from16 v30, v5

    const/4 v5, 0x4

    if-ne v14, v5, :cond_32d

    :try_start_322
    aget-object v5, v10, v19

    invoke-static {v5}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v2
    :try_end_328
    .catch Ljava/lang/NumberFormatException; {:try_start_322 .. :try_end_328} :catch_329

    goto :goto_32e

    :catch_329
    invoke-interface {v9, v15, v2}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_32d
    const/4 v2, 0x0

    :goto_32e
    const-string v5, "b"

    const/16 v18, 0x1

    aget-object v14, v10, v18

    invoke-virtual {v5, v14}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v5

    aget-object v14, v10, v18

    invoke-virtual {v4, v14}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v14

    const/16 v17, 0x2

    aget-object v10, v10, v17

    invoke-virtual {v3, v10}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v10

    sget-boolean v31, Lf/ms5;->T20:Z

    if-eqz v31, :cond_35a

    move/from16 v31, v2

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v2

    move/from16 v32, v5

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v5

    invoke-interface {v9, v13, v2, v5}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_35e

    :cond_35a
    move/from16 v31, v2

    move/from16 v32, v5

    .line 35
    :goto_35e
    sget-object v2, Lf/im7;->Wr0:Lf/im7;

    if-eqz v32, :cond_364

    const/4 v5, -0x1

    goto :goto_369

    :cond_364
    if-eqz v14, :cond_368

    const/4 v5, 0x1

    goto :goto_369

    :cond_368
    const/4 v5, 0x0

    .line 36
    :goto_369
    new-instance v14, Lf/tz;

    .line 37
    invoke-direct {v14, v0}, Lf/ix4;-><init>(Lf/z46;)V

    move/from16 v32, v10

    const/4 v10, 0x0

    iput-object v10, v14, Lf/tz;->di1:[Lf/wr2;

    .line 38
    iget-object v2, v2, Lf/im7;->v71:Lf/k89;

    const/4 v10, 0x1

    if-ne v5, v10, :cond_37d

    or-int/lit8 v10, v31, 0x20

    int-to-byte v10, v10

    move/from16 v31, v10

    :cond_37d
    if-eqz v32, :cond_384

    or-int/lit8 v10, v31, 0x40

    int-to-byte v10, v10

    move/from16 v31, v10

    :cond_384
    const/4 v10, -0x1

    if-eq v5, v10, :cond_398

    shl-int/lit8 v5, v31, 0x10

    or-int v5, v30, v5

    .line 39
    invoke-virtual {v2, v5, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    or-int/lit8 v5, v31, -0x80

    :goto_390
    shl-int/lit8 v5, v5, 0x10

    or-int v5, v30, v5

    invoke-virtual {v2, v5, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    goto :goto_3b4

    :cond_398
    shl-int/lit8 v5, v31, 0x10

    or-int v5, v30, v5

    invoke-virtual {v2, v5, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    or-int/lit8 v5, v31, -0x80

    shl-int/lit8 v5, v5, 0x10

    or-int v5, v30, v5

    invoke-virtual {v2, v5, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    or-int/lit8 v5, v31, 0x20

    shl-int/lit8 v5, v5, 0x10

    or-int v5, v30, v5

    invoke-virtual {v2, v5, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    or-int/lit8 v5, v31, -0x60

    goto :goto_390

    :goto_3b4
    move-object/from16 v2, v27

    goto :goto_3c1

    .line 40
    :catch_3b7
    invoke-interface {v9, v15, v2}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_3bb
    move-object/from16 v28, v10

    move-object/from16 v29, v14

    const/4 v10, -0x1

    goto :goto_3b4

    :goto_3c1
    array-length v5, v2

    const/4 v14, 0x3

    if-ne v5, v14, :cond_46e

    const/16 v22, 0x0

    aget-object v5, v2, v22

    invoke-virtual {v12, v5}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_46e

    const-string v5, "followcostumes"

    const/16 v18, 0x1

    aget-object v14, v2, v18

    invoke-virtual {v5, v14}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_46e

    const/4 v5, 0x2

    aget-object v14, v2, v5

    invoke-virtual {v14, v6}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v14, v11}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v16

    if-nez v16, :cond_3ee

    const-string v0, "Only .png files supported for /sprites/followcostumes/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_3ee
    const/16 v10, 0x2e

    invoke-virtual {v14, v10}, Ljava/lang/String;->indexOf(I)I

    move-result v5

    const/4 v10, 0x0

    invoke-virtual {v14, v10, v5}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v5, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v14

    const/16 v22, 0x0

    array-length v10, v14

    move-object/from16 v27, v14

    const/4 v14, 0x2

    if-ge v10, v14, :cond_40b

    const-string v0, "{} does not have enough fields. Expected name format is ID-FRAME.png"

    invoke-interface {v9, v0, v5}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_40b
    :try_start_40b
    aget-object v10, v27, v22

    invoke-static {v10}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v10

    const/16 v18, 0x1

    aget-object v14, v27, v18

    invoke-static {v14}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v14
    :try_end_419
    .catch Ljava/lang/NumberFormatException; {:try_start_40b .. :try_end_419} :catch_468

    move-object/from16 v27, v15

    const/16 v15, 0x2710

    if-lt v10, v15, :cond_462

    sget-boolean v5, Lf/ms5;->T20:Z

    if-eqz v5, :cond_42e

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v5

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v15

    invoke-interface {v9, v13, v5, v15}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    :cond_42e
    new-instance v5, Lf/tx8;

    invoke-direct {v5, v0}, Lf/tx8;-><init>(Lf/z46;)V

    .line 41
    sget-object v15, Lf/im7;->Wr0:Lf/im7;

    .line 42
    iget-object v15, v15, Lf/im7;->hI1:Lf/k89;

    .line 43
    invoke-virtual {v15, v10}, Lf/k89;->get(I)Ljava/lang/Object;

    move-result-object v30

    check-cast v30, Lf/cd7;

    if-nez v30, :cond_456

    move-object/from16 v31, v13

    new-instance v13, Lf/cd7;

    .line 44
    invoke-direct {v13}, Ljava/lang/Object;-><init>()V

    move-object/from16 v32, v4

    new-instance v4, Lf/k89;

    .line 45
    invoke-direct {v4}, Lf/x44;-><init>()V

    .line 46
    iput-object v4, v13, Lf/cd7;->Tk1:Lf/k89;

    const/4 v4, 0x0

    iput-object v4, v13, Lf/cd7;->yC:[Lf/wr2;

    .line 47
    invoke-virtual {v15, v10, v13}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    goto :goto_45c

    :cond_456
    move-object/from16 v32, v4

    move-object/from16 v31, v13

    move-object/from16 v13, v30

    .line 48
    :goto_45c
    iget-object v4, v13, Lf/cd7;->Tk1:Lf/k89;

    invoke-virtual {v4, v14, v5}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    goto :goto_474

    .line 49
    :cond_462
    :try_start_462
    new-instance v0, Ljava/lang/NumberFormatException;

    invoke-direct {v0}, Ljava/lang/NumberFormatException;-><init>()V

    throw v0
    :try_end_468
    .catch Ljava/lang/NumberFormatException; {:try_start_462 .. :try_end_468} :catch_468

    :catch_468
    const-string v0, "{} has an invalid sprite_id."

    invoke-interface {v9, v0, v5}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_46e
    move-object/from16 v32, v4

    move-object/from16 v31, v13

    move-object/from16 v27, v15

    :goto_474
    array-length v4, v2

    const-string v5, "m"

    const-string v13, "Malformed ID for egg sprite: {}"

    const-string v14, "[_.]"

    const/4 v15, 0x3

    if-ne v4, v15, :cond_6f8

    const/16 v22, 0x0

    aget-object v4, v2, v22

    invoke-virtual {v12, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :cond_6f8

    const-string v4, "battlesprites"

    const/16 v18, 0x1

    aget-object v15, v2, v18

    invoke-virtual {v4, v15}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :cond_6f8

    const/16 v17, 0x2

    aget-object v4, v2, v17

    invoke-virtual {v4, v6}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v4

    const-string v15, "table-front-scale.txt"

    invoke-virtual {v15, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v15

    if-eqz v15, :cond_4a9

    const/4 v15, 0x0

    invoke-static {v0, v15}, Lf/c85;->g70(Lf/z46;B)V

    return-void

    :cond_4a9
    const-string v15, "table-back-scale.txt"

    invoke-virtual {v15, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v15

    if-eqz v15, :cond_4b6

    const/4 v15, 0x1

    invoke-static {v0, v15}, Lf/c85;->g70(Lf/z46;B)V

    return-void

    :cond_4b6
    const-string v15, "table-summary-scale.txt"

    invoke-virtual {v15, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v15

    if-eqz v15, :cond_4c3

    const/4 v15, 0x2

    invoke-static {v0, v15}, Lf/c85;->g70(Lf/z46;B)V

    return-void

    :cond_4c3
    const-string v15, "table-sprite-timings.txt"

    invoke-virtual {v15, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v15

    if-eqz v15, :cond_55c

    const/4 v15, 0x0

    .line 50
    invoke-virtual {v0, v15}, Lf/z46;->xN1(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 51
    invoke-virtual {v0, v8}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    array-length v1, v0

    const/4 v2, 0x0

    :goto_4d6
    if-ge v2, v1, :cond_2e5

    aget-object v3, v0, v2

    invoke-virtual {v3, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :cond_4e8

    move-object/from16 v15, p0

    move-object/from16 v10, v28

    move-object/from16 v4, v29

    goto/16 :goto_554

    :cond_4e8
    invoke-virtual {v3, v7}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v4

    if-eqz v4, :cond_4f8

    const/16 v4, 0x3b

    invoke-virtual {v3, v4}, Ljava/lang/String;->indexOf(I)I

    move-result v4

    invoke-virtual {v3, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v3

    :cond_4f8
    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    move-object/from16 v4, v29

    invoke-virtual {v3, v4}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v5

    if-nez v5, :cond_509

    :goto_504
    move-object/from16 v15, p0

    move-object/from16 v10, v28

    goto :goto_554

    :cond_509
    invoke-virtual {v3, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v5

    array-length v6, v5

    const-string v8, "Invalid battle sprite timings table value. {}"

    const/4 v14, 0x2

    if-ge v6, v14, :cond_517

    invoke-interface {v9, v8, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    goto :goto_504

    :cond_517
    const/16 v22, 0x0

    :try_start_519
    aget-object v6, v5, v22

    invoke-virtual {v6}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v6

    invoke-static {v6}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v6

    const/16 v18, 0x1

    aget-object v5, v5, v18

    invoke-virtual {v5}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v5
    :try_end_52b
    .catch Ljava/lang/NumberFormatException; {:try_start_519 .. :try_end_52b} :catch_54d

    move-object/from16 v10, v28

    :try_start_52d
    invoke-virtual {v5, v10}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v5

    array-length v11, v5

    new-array v12, v11, [I

    const/4 v13, 0x0

    :goto_535
    if-ge v13, v11, :cond_545

    aget-object v14, v5, v13

    invoke-static {v14}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v14

    aput v14, v12, v13
    :try_end_53f
    .catch Ljava/lang/NumberFormatException; {:try_start_52d .. :try_end_53f} :catch_542

    add-int/lit8 v13, v13, 0x1

    goto :goto_535

    :catch_542
    move-object/from16 v15, p0

    goto :goto_551

    :cond_545
    move-object/from16 v15, p0

    iget-object v3, v15, Lf/c85;->Vb0:Lf/ch4;

    invoke-virtual {v3, v6, v12}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    goto :goto_554

    :catch_54d
    move-object/from16 v15, p0

    move-object/from16 v10, v28

    :goto_551
    invoke-interface {v9, v8, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    :goto_554
    add-int/lit8 v2, v2, 0x1

    move-object/from16 v29, v4

    move-object/from16 v28, v10

    goto/16 :goto_4d6

    :cond_55c
    move-object/from16 v15, p0

    .line 52
    const-string v7, "table-coordinate-mods.txt"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_56a

    invoke-static {v0}, Lf/c85;->gD(Lf/z46;)V

    return-void

    :cond_56a
    const-string v7, "dummy.png"

    invoke-virtual {v7, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    if-eqz v7, :cond_587

    .line 53
    sget-object v33, Lf/pr;->Sb1:Lf/pr;

    .line 54
    new-instance v1, Lf/tx8;

    invoke-direct {v1, v0}, Lf/tx8;-><init>(Lf/z46;)V

    const/16 v34, 0x0

    const/16 v35, 0x0

    const/16 v36, 0x0

    const/16 v37, 0x2

    move-object/from16 v38, v1

    invoke-virtual/range {v33 .. v38}, Lf/pr;->kx0(SZZBLf/ix4;)V

    return-void

    :cond_587
    const-string v7, "egg_preview_"

    invoke-virtual {v4, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v7

    if-eqz v7, :cond_5ae

    :try_start_58f
    invoke-virtual {v4, v14}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    const/16 v17, 0x2

    aget-object v1, v1, v17

    invoke-static {v1}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v1

    .line 55
    sget-object v2, Lf/pr;->Sb1:Lf/pr;

    .line 56
    new-instance v3, Lf/mo8;

    const/4 v7, 0x0

    invoke-direct {v3, v0, v7}, Lf/mo8;-><init>(Lf/z46;[I)V

    .line 57
    iget-object v0, v2, Lf/pr;->Vs1:Lf/k89;

    .line 58
    invoke-virtual {v0, v1, v3}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;
    :try_end_5a8
    .catch Ljava/lang/Exception; {:try_start_58f .. :try_end_5a8} :catch_5a9

    return-void

    :catch_5a9
    move-exception v0

    .line 59
    invoke-interface {v9, v13, v4, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_5ae
    const/4 v7, 0x0

    invoke-virtual {v4, v11}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v8

    if-nez v8, :cond_5c3

    const-string v8, ".gif"

    invoke-virtual {v4, v8}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v8

    if-nez v8, :cond_5c3

    const-string v0, "Only .png/.gif files supported for /sprites/battlesprites/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_5c3
    const/16 v8, 0x2e

    invoke-virtual {v4, v8}, Ljava/lang/String;->indexOf(I)I

    move-result v7

    const/4 v8, 0x0

    invoke-virtual {v4, v8, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v7

    array-length v8, v7

    const/4 v10, 0x3

    if-ge v8, v10, :cond_5dc

    const-string v0, "{} does not have enough fields. Expected name format is ID-back-s.png where \'back\' is either \'back\' or \'front\', and \'s\' is \'s\' or \'n\'."

    invoke-interface {v9, v0, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_5dc
    array-length v8, v7

    const-string v10, "PNG ONLY/OPTIONAL: frame is \'0\' - \'127\'"

    move-object/from16 v29, v1

    const-string v1, "OPTIONAL: gender is \'m\' or \'f\'"

    move-object/from16 v30, v13

    const-string v13, "\'s\' is \'s\' or \'n\' (Shiny/Normal)"

    move-object/from16 v33, v14

    const-string v14, "\'back\' is either \'back\' or \'front\' (Ally/Enemy)"

    move-object/from16 v34, v11

    const-string v11, "{} invalid file name. Expected name format is ID-back-shiny-gender-frame.png where:"

    move-object/from16 v35, v6

    const/4 v6, 0x5

    if-le v8, v6, :cond_604

    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v14}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v13}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_604
    const-string v6, "back"

    const/16 v18, 0x1

    aget-object v8, v7, v18

    invoke-virtual {v6, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v37

    const/16 v17, 0x2

    aget-object v6, v7, v17

    invoke-virtual {v3, v6}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v40

    const/4 v8, 0x0

    :try_start_617
    aget-object v6, v7, v8

    invoke-static {v6}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v36
    :try_end_61d
    .catch Ljava/lang/Exception; {:try_start_617 .. :try_end_61d} :catch_6e8

    array-length v1, v7

    const-string v6, "gender is \'m\' or \'f\'"

    const-string v10, "frame is \'0\' - \'127\'"

    const/4 v13, 0x4

    if-ne v1, v13, :cond_660

    const/16 v19, 0x3

    aget-object v1, v7, v19

    invoke-virtual {v1, v8}, Ljava/lang/String;->charAt(I)C

    move-result v7

    invoke-static {v7}, Ljava/lang/Character;->isDigit(C)Z

    move-result v7

    if-eqz v7, :cond_643

    :try_start_633
    invoke-static {v1}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v1
    :try_end_637
    .catch Ljava/lang/NumberFormatException; {:try_start_633 .. :try_end_637} :catch_63c

    move v6, v1

    move-object/from16 v8, v32

    const/4 v1, 0x2

    goto :goto_656

    :catch_63c
    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_643
    invoke-virtual {v5, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v7

    move-object/from16 v8, v32

    if-eqz v7, :cond_64e

    const/4 v1, 0x0

    :goto_64c
    const/4 v6, 0x0

    goto :goto_656

    :cond_64e
    invoke-virtual {v8, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_659

    const/4 v1, 0x1

    goto :goto_64c

    :goto_656
    move/from16 v39, v1

    goto :goto_694

    :cond_659
    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v6}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_660
    move-object/from16 v8, v32

    array-length v1, v7

    const/4 v13, 0x5

    if-ne v1, v13, :cond_691

    const/16 v19, 0x3

    aget-object v1, v7, v19

    invoke-virtual {v5, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_674

    const/4 v1, 0x0

    :goto_671
    const/16 v26, 0x4

    goto :goto_67c

    :cond_674
    invoke-virtual {v8, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_68a

    const/4 v1, 0x1

    goto :goto_671

    :goto_67c
    :try_start_67c
    aget-object v6, v7, v26

    invoke-static {v6}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v6
    :try_end_682
    .catch Ljava/lang/NumberFormatException; {:try_start_67c .. :try_end_682} :catch_683

    goto :goto_656

    :catch_683
    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_68a
    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v6}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_691
    const/4 v6, 0x0

    const/16 v39, 0x2

    :goto_694
    if-lez v6, :cond_6a8

    invoke-virtual {v0}, Lf/z46;->XV0()Ljava/lang/String;

    move-result-object v1

    const-string v7, "gif"

    invoke-virtual {v1, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_6a8

    const-string v0, "{} error: GIF format does not support frame ids. Remove frame id"

    invoke-interface {v9, v0, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_6a8
    const/16 v38, 0x0

    const/16 v41, 0x0

    .line 60
    invoke-static/range {v36 .. v41}, Lf/pr;->TO1(SZZBZZ)I

    move-result v1

    move/from16 v10, v36

    move/from16 v4, v37

    move/from16 v11, v39

    move/from16 v7, v40

    .line 61
    iget-object v13, v15, Lf/c85;->wH1:Lf/k89;

    invoke-virtual {v13, v1}, Lf/k89;->get(I)Ljava/lang/Object;

    move-result-object v14

    check-cast v14, Lf/wp4;

    if-nez v14, :cond_6dc

    new-instance v14, Lf/wp4;

    .line 62
    invoke-direct {v14}, Ljava/lang/Object;-><init>()V

    move/from16 v32, v6

    new-instance v6, Ljava/util/TreeMap;

    invoke-direct {v6}, Ljava/util/TreeMap;-><init>()V

    iput-object v6, v14, Lf/wp4;->iN1:Ljava/util/TreeMap;

    iput-short v10, v14, Lf/wp4;->cc0:S

    iput-boolean v4, v14, Lf/wp4;->q31:Z

    iput-boolean v7, v14, Lf/wp4;->JQ0:Z

    iput-byte v11, v14, Lf/wp4;->la:B

    .line 63
    invoke-virtual {v13, v1, v14}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    goto :goto_6de

    :cond_6dc
    move/from16 v32, v6

    .line 64
    :goto_6de
    iget-object v1, v14, Lf/wp4;->iN1:Ljava/util/TreeMap;

    invoke-static/range {v32 .. v32}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    invoke-virtual {v1, v4, v0}, Ljava/util/TreeMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    goto :goto_706

    .line 65
    :catch_6e8
    invoke-interface {v9, v11, v4}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v14}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v13}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_6f8
    move-object/from16 v15, p0

    move-object/from16 v29, v1

    move-object/from16 v35, v6

    move-object/from16 v34, v11

    move-object/from16 v30, v13

    move-object/from16 v33, v14

    move-object/from16 v8, v32

    :goto_706
    array-length v1, v2

    const/4 v14, 0x3

    if-ne v1, v14, :cond_8b7

    const/16 v22, 0x0

    aget-object v1, v2, v22

    invoke-virtual {v12, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_8b7

    const-string v1, "monstericons"

    const/16 v18, 0x1

    aget-object v4, v2, v18

    invoke-virtual {v1, v4}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_8b7

    const/16 v17, 0x2

    aget-object v1, v2, v17

    move-object/from16 v4, v35

    invoke-virtual {v1, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v1

    move-object/from16 v6, v34

    invoke-virtual {v1, v6}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_738

    const-string v0, "Only .png files supported for /sprites/monstericons/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_738
    const-string v7, "egg_icon_"

    invoke-virtual {v1, v7}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v7

    if-eqz v7, :cond_764

    move-object/from16 v7, v33

    :try_start_742
    invoke-virtual {v1, v7}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v2

    const/16 v17, 0x2

    aget-object v2, v2, v17

    invoke-static {v2}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v2

    .line 66
    sget-object v3, Lf/pr;->Sb1:Lf/pr;

    .line 67
    new-instance v4, Lf/tx8;

    invoke-direct {v4, v0}, Lf/tx8;-><init>(Lf/z46;)V

    .line 68
    iget-object v0, v3, Lf/pr;->KX:Lf/k89;

    .line 69
    invoke-virtual {v0, v2, v4}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;
    :try_end_75a
    .catch Ljava/lang/Exception; {:try_start_742 .. :try_end_75a} :catch_75e

    return-void

    :goto_75b
    move-object/from16 v10, v30

    goto :goto_760

    :catch_75e
    move-exception v0

    goto :goto_75b

    .line 70
    :goto_760
    invoke-interface {v9, v10, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_764
    move-object/from16 v10, v30

    move-object/from16 v7, v33

    const-string v11, "composite-"

    invoke-virtual {v1, v11}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v11

    if-eqz v11, :cond_7a0

    move-object/from16 v11, v29

    invoke-virtual {v1, v11}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v13

    const/16 v18, 0x1

    :try_start_778
    aget-object v13, v13, v18

    invoke-static {v13}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v13
    :try_end_77e
    .catch Ljava/lang/Exception; {:try_start_778 .. :try_end_77e} :catch_799

    const/16 v14, 0x2d

    invoke-virtual {v1, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v29

    move/from16 v30, v13

    add-int/lit8 v13, v29, 0x1

    invoke-virtual {v1, v14, v13}, Ljava/lang/String;->indexOf(II)I

    move-result v13

    add-int/lit8 v13, v13, 0x1

    invoke-virtual {v1, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v1

    move/from16 v33, v30

    const/16 v29, 0x1

    :goto_796
    const/16 v14, 0x2e

    goto :goto_7a7

    :catch_799
    move-exception v0

    const-string v2, "Malformed composite base for sprite: {}"

    invoke-interface {v9, v2, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_7a0
    move-object/from16 v11, v29

    const/16 v29, 0x0

    const/16 v33, 0x0

    goto :goto_796

    :goto_7a7
    invoke-virtual {v1, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v13

    const/4 v14, 0x0

    invoke-virtual {v1, v14, v13}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v11}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v13

    array-length v14, v13

    const-string v15, "OPTIONAL: \'s\' denotes a shiny sprite"

    move-object/from16 v30, v11

    const-string v11, "OPTIONAL: G is gender - \'m\' or \'f\'"

    move-object/from16 v39, v10

    const-string v10, "F is frame id"

    move-object/from16 v40, v7

    const-string v7, "{} does not have enough fields. Expected name format is ID-F-G-s.png"

    move-object/from16 v41, v6

    const/4 v6, 0x2

    if-ge v14, v6, :cond_7d5

    invoke-interface {v9, v7, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v11}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v15}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_7d5
    const/16 v22, 0x0

    :try_start_7d7
    aget-object v6, v13, v22

    invoke-static {v6}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v34
    :try_end_7dd
    .catch Ljava/lang/NumberFormatException; {:try_start_7d7 .. :try_end_7dd} :catch_8b1

    const/16 v18, 0x1

    :try_start_7df
    aget-object v6, v13, v18

    invoke-static {v6}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v35
    :try_end_7e5
    .catch Ljava/lang/NumberFormatException; {:try_start_7df .. :try_end_7e5} :catch_8ab

    array-length v6, v13

    const/4 v14, 0x3

    if-lt v6, v14, :cond_83d

    const/16 v17, 0x2

    aget-object v6, v13, v17

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_7f7

    const/4 v5, 0x0

    const/16 v16, 0x0

    goto :goto_80d

    :cond_7f7
    invoke-virtual {v8, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_801

    const/4 v5, 0x0

    const/16 v16, 0x1

    goto :goto_80d

    :cond_801
    invoke-virtual {v3, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_830

    array-length v5, v13

    if-ne v5, v14, :cond_830

    const/4 v5, 0x1

    const/16 v16, -0x1

    :goto_80d
    array-length v6, v13

    const/4 v8, 0x4

    if-lt v6, v8, :cond_82b

    aget-object v5, v13, v14

    invoke-virtual {v3, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_81e

    move/from16 v45, v16

    const/16 v46, 0x1

    goto :goto_841

    :cond_81e
    invoke-interface {v9, v7, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v11}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v15}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_82b
    move/from16 v46, v5

    move/from16 v45, v16

    goto :goto_841

    :cond_830
    invoke-interface {v9, v7, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    invoke-interface {v9, v10}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v11}, Lf/xv7;->info(Ljava/lang/String;)V

    invoke-interface {v9, v15}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_83d
    const/16 v45, -0x1

    const/16 v46, 0x0

    :goto_841
    sget-boolean v1, Lf/ms5;->T20:Z

    if-eqz v1, :cond_853

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v1

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v3

    move-object/from16 v5, v31

    invoke-interface {v9, v5, v1, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_855

    :cond_853
    move-object/from16 v5, v31

    :goto_855
    if-eqz v29, :cond_880

    if-ltz v45, :cond_86a

    .line 71
    sget-object v32, Lf/pr;->Sb1:Lf/pr;

    .line 72
    new-instance v1, Lf/tx8;

    invoke-direct {v1, v0}, Lf/tx8;-><init>(Lf/z46;)V

    move-object/from16 v38, v1

    move/from16 v36, v45

    move/from16 v37, v46

    invoke-virtual/range {v32 .. v38}, Lf/pr;->K60(SSBBZLf/tx8;)V

    goto :goto_8c3

    .line 73
    :cond_86a
    sget-object v32, Lf/pr;->Sb1:Lf/pr;

    .line 74
    new-instance v1, Lf/tx8;

    invoke-direct {v1, v0}, Lf/tx8;-><init>(Lf/z46;)V

    const/16 v36, 0x0

    move-object/from16 v38, v1

    move/from16 v37, v46

    .line 75
    invoke-virtual/range {v32 .. v38}, Lf/pr;->K60(SSBBZLf/tx8;)V

    const/16 v36, 0x1

    .line 76
    invoke-virtual/range {v32 .. v38}, Lf/pr;->K60(SSBBZLf/tx8;)V

    goto :goto_8c3

    :cond_880
    if-ltz v45, :cond_893

    .line 77
    sget-object v42, Lf/pr;->Sb1:Lf/pr;

    .line 78
    new-instance v1, Lf/tx8;

    invoke-direct {v1, v0}, Lf/tx8;-><init>(Lf/z46;)V

    move-object/from16 v47, v1

    move/from16 v43, v34

    move/from16 v44, v35

    invoke-virtual/range {v42 .. v47}, Lf/pr;->Je1(SBBZLf/tx8;)V

    goto :goto_8c3

    .line 79
    :cond_893
    sget-object v42, Lf/pr;->Sb1:Lf/pr;

    .line 80
    new-instance v1, Lf/tx8;

    invoke-direct {v1, v0}, Lf/tx8;-><init>(Lf/z46;)V

    const/16 v45, 0x0

    move-object/from16 v47, v1

    move/from16 v43, v34

    move/from16 v44, v35

    .line 81
    invoke-virtual/range {v42 .. v47}, Lf/pr;->Je1(SBBZLf/tx8;)V

    const/16 v45, 0x1

    .line 82
    invoke-virtual/range {v42 .. v47}, Lf/pr;->Je1(SBBZLf/tx8;)V

    goto :goto_8c3

    .line 83
    :catch_8ab
    const-string v0, "{} has an invalid frame id."

    invoke-interface {v9, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :catch_8b1
    move-object/from16 v0, v27

    invoke-interface {v9, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_8b7
    move-object/from16 v39, v30

    move-object/from16 v5, v31

    move-object/from16 v40, v33

    move-object/from16 v41, v34

    move-object/from16 v4, v35

    move-object/from16 v30, v29

    :goto_8c3
    array-length v1, v2

    const/4 v14, 0x3

    if-ne v1, v14, :cond_93f

    const/16 v22, 0x0

    aget-object v1, v2, v22

    invoke-virtual {v12, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_93f

    const-string v1, "eggsprites"

    const/16 v18, 0x1

    aget-object v3, v2, v18

    invoke-virtual {v1, v3}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_93f

    const/16 v17, 0x2

    aget-object v1, v2, v17

    invoke-virtual {v1, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v1

    move-object/from16 v6, v41

    invoke-virtual {v1, v6}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_8f3

    const-string v0, "Only .png files supported for /sprites/eggsprites/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_8f3
    const-string v3, "egg_"

    invoke-virtual {v1, v3}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v3

    const-string v7, "Expected format: egg_{ID}_{FRAME_NUM}.png"

    if-nez v3, :cond_901

    invoke-interface {v9, v7}, Lf/xv7;->error(Ljava/lang/String;)V

    return-void

    :cond_901
    move-object/from16 v3, v40

    :try_start_903
    invoke-virtual {v1, v3}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v3

    const/16 v18, 0x1

    aget-object v8, v3, v18

    invoke-static {v8}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v8

    const/16 v17, 0x2

    aget-object v3, v3, v17

    invoke-static {v3}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v3

    move-object/from16 v10, v25

    invoke-virtual {v10, v8}, Lf/k89;->get(I)Ljava/lang/Object;

    move-result-object v11

    check-cast v11, Lf/yb1;

    if-nez v11, :cond_92e

    new-instance v11, Lf/yb1;

    invoke-direct {v11, v8}, Lf/yb1;-><init>(I)V

    invoke-virtual {v10, v8, v11}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    goto :goto_92e

    :catch_92a
    move-exception v0

    move-object/from16 v10, v39

    goto :goto_938

    .line 84
    :cond_92e
    :goto_92e
    iget-object v8, v11, Lf/yb1;->Kl0:Ljava/util/TreeMap;

    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-virtual {v8, v3, v0}, Ljava/util/TreeMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_937
    .catch Ljava/lang/Exception; {:try_start_903 .. :try_end_937} :catch_92a

    goto :goto_941

    .line 85
    :goto_938
    invoke-interface {v9, v10, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    invoke-interface {v9, v7}, Lf/xv7;->error(Ljava/lang/String;)V

    return-void

    :cond_93f
    move-object/from16 v6, v41

    :goto_941
    array-length v1, v2

    const/16 v3, 0xa

    const/4 v8, 0x4

    if-ne v1, v8, :cond_a89

    const/16 v22, 0x0

    aget-object v1, v2, v22

    invoke-virtual {v12, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_a89

    const-string v1, "overworldsprites"

    const/16 v18, 0x1

    aget-object v7, v2, v18

    invoke-virtual {v1, v7}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_a89

    const/16 v19, 0x3

    aget-object v1, v2, v19

    invoke-virtual {v1, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1, v6}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_971

    const-string v0, "Only .png files supported for /sprites/overworldsprites/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_971
    const/16 v14, 0x2e

    invoke-virtual {v1, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v7

    const/4 v8, 0x0

    invoke-virtual {v1, v8, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v1

    const/16 v17, 0x2

    :try_start_97e
    aget-object v7, v2, v17
    :try_end_980
    .catch Ljava/lang/NumberFormatException; {:try_start_97e .. :try_end_980} :catch_a7f

    :try_start_980
    invoke-static {v7}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v7
    :try_end_984
    .catch Ljava/lang/NumberFormatException; {:try_start_980 .. :try_end_984} :catch_a7d

    if-ltz v7, :cond_98d

    const/4 v13, 0x5

    if-le v7, v13, :cond_98a

    goto :goto_98d

    :cond_98a
    :goto_98a
    move-object/from16 v11, v30

    goto :goto_990

    :cond_98d
    :goto_98d
    if-ne v7, v3, :cond_a71

    goto :goto_98a

    :goto_990
    invoke-virtual {v1, v11}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v8

    array-length v10, v8

    const/4 v14, 0x2

    if-ge v10, v14, :cond_9a2

    const-string v0, "overworldsprites/{}/{} does not have enough fields. Expected name format is ID-F.png where \'F\' is frame id."

    invoke-static {v7}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v2

    invoke-interface {v9, v0, v2, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_9a2
    const/16 v22, 0x0

    :try_start_9a4
    aget-object v10, v8, v22

    invoke-static {v10}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v10
    :try_end_9aa
    .catch Ljava/lang/NumberFormatException; {:try_start_9a4 .. :try_end_9aa} :catch_a63

    const/16 v18, 0x1

    :try_start_9ac
    aget-object v13, v8, v18

    invoke-static {v13}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v13
    :try_end_9b2
    .catch Ljava/lang/NumberFormatException; {:try_start_9ac .. :try_end_9b2} :catch_a44

    array-length v14, v8

    const/4 v15, 0x2

    if-le v14, v15, :cond_9c2

    aget-object v8, v8, v15

    const-string v14, "glowoverlay"

    invoke-virtual {v8, v14}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v8

    if-eqz v8, :cond_9c2

    const/4 v8, 0x1

    goto :goto_9c3

    :cond_9c2
    const/4 v8, 0x0

    :goto_9c3
    sget-boolean v14, Lf/ms5;->T20:Z

    if-eqz v14, :cond_9d2

    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v14

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v15

    invoke-interface {v9, v5, v14, v15}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    :cond_9d2
    new-instance v14, Lf/tx8;

    invoke-direct {v14, v0}, Lf/tx8;-><init>(Lf/z46;)V

    .line 86
    sget-object v15, Lf/f2;->rt1:Lf/f2;

    .line 87
    invoke-virtual {v15, v7, v10, v8}, Lf/f2;->bh0(BIZ)Lf/cc6;

    move-result-object v3

    move/from16 v25, v7

    if-eqz v3, :cond_9ed

    sget-object v7, Lf/f2;->Ai1:Lf/bm8;

    if-eq v3, v7, :cond_9ed

    .line 88
    instance-of v7, v3, Lf/bm8;

    if-eqz v7, :cond_9ea

    goto :goto_9ed

    :cond_9ea
    move-object/from16 v29, v11

    goto :goto_a0b

    .line 89
    :cond_9ed
    :goto_9ed
    new-instance v3, Lf/x12;

    invoke-direct {v3}, Lf/x12;-><init>()V

    move-object/from16 v29, v11

    const/16 v7, 0xa

    const/4 v11, 0x0

    .line 90
    invoke-virtual {v15, v7, v10, v11}, Lf/f2;->bh0(BIZ)Lf/cc6;

    move-result-object v0

    .line 91
    iget-boolean v0, v0, Lf/cc6;->qO:Z

    if-eqz v0, :cond_a00

    goto :goto_a0b

    .line 92
    :cond_a00
    iget-object v0, v15, Lf/f2;->Xf:Lf/k33;

    invoke-virtual {v0, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lf/l74;

    invoke-virtual {v0, v10, v3, v8}, Lf/l74;->Mr(ILf/cc6;Z)V

    .line 93
    :goto_a0b
    iget-boolean v0, v3, Lf/cc6;->qO:Z

    move-object/from16 v15, p0

    if-eqz v0, :cond_a1f

    .line 94
    iget-boolean v0, v15, Lf/c85;->qc1:Z

    if-nez v0, :cond_dc3

    const-string v0, "overworldsprites/{}/{} cannot be modified."

    invoke-static/range {v25 .. v25}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v2

    invoke-interface {v9, v0, v2, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_a1f
    check-cast v3, Lf/x12;

    new-instance v0, Lf/qj6;

    new-instance v1, Lf/tn5;

    const/16 v7, 0x17

    invoke-direct {v1, v7, v14}, Lf/tn5;-><init>(ILjava/lang/Object;)V

    invoke-direct {v0, v1}, Lf/qj6;-><init>(Lf/ll1;)V

    .line 95
    iget-boolean v1, v3, Lf/cc6;->qO:Z

    if-nez v1, :cond_a3e

    .line 96
    iget-object v1, v3, Lf/x12;->de0:Lf/k89;

    invoke-virtual {v1, v13, v0}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    iget-object v0, v3, Lf/x12;->uo0:[Lf/qj6;

    if-eqz v0, :cond_a8d

    invoke-virtual {v3}, Lf/x12;->Ar()V

    goto :goto_a8d

    :cond_a3e
    const-string v0, "Cannot add frame to locked overworld sprite"

    invoke-static {v0}, Lf/i82;->rE0(Ljava/lang/String;)V

    return-void

    :catch_a44
    move-object/from16 v15, p0

    move/from16 v25, v7

    .line 97
    invoke-static/range {v25 .. v25}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v0

    const/16 v18, 0x1

    aget-object v2, v8, v18

    const/4 v14, 0x3

    new-array v3, v14, [Ljava/lang/Object;

    const/16 v22, 0x0

    aput-object v0, v3, v22

    aput-object v1, v3, v18

    const/16 v17, 0x2

    aput-object v2, v3, v17

    const-string v0, "overworldsprites/{}/{} has an invalid frame id: Failed to parse {}"

    invoke-interface {v9, v0, v3}, Lf/xv7;->info(Ljava/lang/String;[Ljava/lang/Object;)V

    return-void

    :catch_a63
    move-object/from16 v15, p0

    move/from16 v25, v7

    const-string v0, "overworldsprites/{}/{} has an invalid overworld sprite id."

    invoke-static/range {v25 .. v25}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    move-result-object v2

    invoke-interface {v9, v0, v2, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_a71
    move-object/from16 v15, p0

    const/16 v17, 0x2

    aget-object v0, v2, v17

    move-object/from16 v3, v23

    invoke-interface {v9, v3, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :catch_a7d
    const/16 v17, 0x2

    :catch_a7f
    move-object/from16 v15, p0

    move-object/from16 v3, v23

    aget-object v0, v2, v17

    invoke-interface {v9, v3, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_a89
    move-object/from16 v15, p0

    move-object/from16 v29, v30

    :cond_a8d
    :goto_a8d
    array-length v0, v2

    const/4 v8, 0x4

    if-ne v0, v8, :cond_b34

    const/16 v22, 0x0

    aget-object v0, v2, v22

    invoke-virtual {v12, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_b34

    const-string v0, "trainersprites"

    const/16 v18, 0x1

    aget-object v1, v2, v18

    invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_b34

    const/16 v19, 0x3

    aget-object v0, v2, v19

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v6}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-nez v1, :cond_abb

    const-string v0, "Only .png files supported for /sprites/trainersprites/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_abb
    const/16 v17, 0x2

    :try_start_abd
    aget-object v1, v2, v17

    invoke-static {v1}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v1
    :try_end_ac3
    .catch Ljava/lang/NumberFormatException; {:try_start_abd .. :try_end_ac3} :catch_b2b

    if-ltz v1, :cond_ac8

    const/4 v13, 0x5

    if-le v1, v13, :cond_acb

    :cond_ac8
    const/16 v7, 0xa

    goto :goto_ace

    :cond_acb
    :goto_acb
    const/16 v14, 0x2e

    goto :goto_ad1

    :goto_ace
    if-ne v1, v7, :cond_b22

    goto :goto_acb

    :goto_ad1
    invoke-virtual {v0, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v3

    const/4 v8, 0x0

    invoke-virtual {v0, v8, v3}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    :try_start_ada
    invoke-static {v0}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0
    :try_end_ade
    .catch Ljava/lang/NumberFormatException; {:try_start_ada .. :try_end_ade} :catch_b1c

    sget-boolean v3, Lf/ms5;->T20:Z

    if-eqz v3, :cond_aed

    invoke-virtual/range {p1 .. p1}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v3

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v6

    invoke-interface {v9, v5, v3, v6}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    :cond_aed
    new-instance v3, Lf/tx8;

    move-object/from16 v6, p1

    invoke-direct {v3, v6}, Lf/tx8;-><init>(Lf/z46;)V

    invoke-static {}, Lf/ye4;->wj1()Lf/ye4;

    move-result-object v7

    new-instance v8, Lf/qj6;

    new-instance v10, Lf/yy0;

    const/4 v14, 0x3

    invoke-direct {v10, v3, v14}, Lf/yy0;-><init>(Lf/tx8;I)V

    invoke-direct {v8, v10}, Lf/qj6;-><init>(Lf/ll1;)V

    .line 98
    iget-object v3, v7, Lf/ye4;->sp:Lf/k33;

    .line 99
    invoke-virtual {v3, v1}, Lf/k33;->VK0(B)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Lf/st;

    if-nez v7, :cond_b15

    new-instance v7, Lf/st;

    invoke-direct {v7}, Lf/st;-><init>()V

    invoke-virtual {v3, v1, v7}, Lf/k33;->xp1(BLjava/lang/Object;)Ljava/lang/Object;

    .line 100
    :cond_b15
    iget-object v1, v7, Lf/st;->xY:Lf/k89;

    invoke-virtual {v1, v0, v8}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    :goto_b1a
    const/4 v8, 0x2

    goto :goto_b37

    .line 101
    :catch_b1c
    const-string v1, "{} has an invalid trainer sprite id."

    invoke-interface {v9, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_b22
    const/4 v8, 0x2

    aget-object v1, v2, v8

    move-object/from16 v3, v21

    invoke-interface {v9, v3, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :catch_b2b
    move-object/from16 v3, v21

    const/4 v8, 0x2

    aget-object v1, v2, v8

    invoke-interface {v9, v3, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    return-void

    :cond_b34
    move-object/from16 v6, p1

    goto :goto_b1a

    :goto_b37
    array-length v0, v2

    const-string v1, ".wav"

    if-eq v0, v8, :cond_b41

    array-length v0, v2

    const/4 v14, 0x3

    if-ne v0, v14, :cond_bc8

    goto :goto_b42

    :cond_b41
    const/4 v14, 0x3

    :goto_b42
    const-string v0, "sounds"

    const/16 v22, 0x0

    aget-object v3, v2, v22

    invoke-virtual {v0, v3}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_bc8

    array-length v0, v2

    const/16 v18, 0x1

    if-ne v0, v14, :cond_b5a

    aget-object v0, v2, v18

    invoke-static {v0}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v0

    goto :goto_b5b

    :cond_b5a
    const/4 v0, 0x0

    :goto_b5b
    array-length v3, v2

    add-int/lit8 v3, v3, -0x1

    aget-object v3, v2, v3

    invoke-virtual {v3, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    const-string v8, ".ogg"

    if-nez v7, :cond_b80

    const-string v7, ".mp3"

    invoke-virtual {v3, v7}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_b80

    invoke-virtual {v3, v8}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_b80

    const-string v0, "Only .wav/.mp3/.ogg files supported for /sounds/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_b80
    const/16 v14, 0x2e

    invoke-virtual {v3, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v7

    const/4 v14, 0x0

    invoke-virtual {v3, v14, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    :try_start_b8b
    invoke-static {v3}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v7
    :try_end_b8f
    .catch Ljava/lang/NumberFormatException; {:try_start_b8b .. :try_end_b8f} :catch_bc2

    invoke-virtual {v3, v8}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v3

    if-eqz v3, :cond_ba6

    sget-object v3, Lf/dq7;->Et:Lf/nx3;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 102
    sget-object v3, Lf/le3;->oj0:Lf/le3;

    .line 103
    sget-object v8, Lf/le3;->LPT6:Lf/le3;

    if-ne v3, v8, :cond_ba6

    const-string v0, "ogg is not supported on iOS"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_ba6
    sget-object v3, Lf/p37;->X20:Lf/t72;

    .line 104
    iget-object v3, v3, Lf/aw3;->SE0:Lf/k89;

    const/high16 v8, 0x10000

    mul-int v0, v0, v8

    add-int/2addr v0, v7

    .line 105
    invoke-virtual {v3, v0, v6}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    .line 106
    sget-boolean v0, Lf/ms5;->T20:Z

    if-eqz v0, :cond_bc8

    invoke-virtual {v6}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v0

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v3

    invoke-interface {v9, v5, v0, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_bc8

    :catch_bc2
    const-string v0, "{} has an invalid sound id."

    invoke-interface {v9, v0, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_bc8
    :goto_bc8
    array-length v0, v2

    const-string v3, "{} has an invalid cry id."

    const/4 v8, 0x2

    if-ne v0, v8, :cond_c45

    const-string v0, "cries"

    const/4 v8, 0x0

    aget-object v7, v2, v8

    invoke-virtual {v0, v7}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_c45

    const/16 v18, 0x1

    aget-object v0, v2, v18

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-nez v1, :cond_bed

    const-string v0, "Only .wav files supported for /cries/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_bed
    const/16 v14, 0x2e

    invoke-virtual {v0, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v1

    invoke-virtual {v0, v8, v1}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    :try_start_bf7
    invoke-static {v0}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0
    :try_end_bfb
    .catch Ljava/lang/NumberFormatException; {:try_start_bf7 .. :try_end_bfb} :catch_c41

    .line 107
    invoke-virtual {v6}, Lf/z46;->yD()[B

    move-result-object v1

    if-eqz v1, :cond_c25

    array-length v7, v1

    const/4 v13, 0x5

    if-ge v7, v13, :cond_c06

    goto :goto_c25

    :cond_c06
    aget-byte v7, v1, v8

    const/16 v8, 0x52

    if-ne v7, v8, :cond_c25

    const/16 v18, 0x1

    aget-byte v7, v1, v18

    const/16 v8, 0x49

    if-ne v7, v8, :cond_c25

    const/16 v17, 0x2

    aget-byte v7, v1, v17

    const/16 v8, 0x46

    if-ne v7, v8, :cond_c25

    const/16 v19, 0x3

    aget-byte v7, v1, v19

    if-ne v7, v8, :cond_c25

    move-object/from16 v24, v1

    goto :goto_c27

    :cond_c25
    :goto_c25
    const/16 v24, 0x0

    :goto_c27
    if-eqz v24, :cond_c45

    .line 108
    new-instance v1, Lf/xg7;

    .line 109
    invoke-direct {v1, v6}, Lf/ix4;-><init>(Lf/z46;)V

    .line 110
    invoke-static {v0, v1}, Lf/vh7;->Tn(SLf/xg7;)V

    # MonMMO-EX: vh7.qi1 is write-only on this client (mod cries never played on Android), so a
    # cries/<id>.wav is also filed where the client keeps its own bundled sounds - aw3.SE0 under region
    # 10, key 10<<16 | id - and g10.run plays it from there. Same thread and map as the sounds/ branch
    # above; the ids (668+) sit far above the client's own sounds/10 files (1..22).
    sget-object v1, Lf/p37;->X20:Lf/t72;
    iget-object v1, v1, Lf/aw3;->SE0:Lf/k89;
    const/high16 v7, 0xa0000
    add-int/2addr v7, v0
    invoke-virtual {v1, v7, v6}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    sget-boolean v0, Lf/ms5;->T20:Z

    if-eqz v0, :cond_c45

    invoke-virtual {v6}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v0

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v1

    invoke-interface {v9, v5, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_c45

    :catch_c41
    invoke-interface {v9, v3, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_c45
    :goto_c45
    array-length v0, v2

    const-string v1, "Only .bin files supported for /world_map_footers/"

    const-string v7, ".bin"

    const/4 v8, 0x2

    if-ne v0, v8, :cond_cc2

    const-string v0, "world_map_footers"

    const/4 v8, 0x0

    aget-object v10, v2, v8

    invoke-virtual {v0, v10}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_cc2

    const/16 v18, 0x1

    aget-object v0, v2, v18

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v10

    if-nez v10, :cond_c6a

    invoke-interface {v9, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_c6a
    const/16 v14, 0x2e

    invoke-virtual {v0, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v10

    invoke-virtual {v0, v8, v10}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    move-object/from16 v11, v29

    invoke-virtual {v0, v11}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v10

    :try_start_c7a
    array-length v11, v10

    const/4 v12, 0x1

    if-le v11, v12, :cond_c8b

    aget-object v11, v10, v8

    invoke-static {v11}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v8

    aget-object v10, v10, v12

    invoke-static {v10}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0

    goto :goto_c94

    :cond_c8b
    const/16 v22, 0x0

    aget-object v8, v10, v22

    invoke-static {v8}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0
    :try_end_c93
    .catch Ljava/lang/NumberFormatException; {:try_start_c7a .. :try_end_c93} :catch_cbc

    const/4 v8, 0x0

    :goto_c94
    invoke-virtual {v6}, Lf/z46;->yD()[B

    move-result-object v10

    invoke-static {v10}, Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;

    move-result-object v10

    sget-object v11, Ljava/nio/ByteOrder;->LITTLE_ENDIAN:Ljava/nio/ByteOrder;

    invoke-virtual {v10, v11}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    move-result-object v10

    new-instance v11, Lf/ev5;

    invoke-direct {v11, v8, v0, v10}, Lf/ev5;-><init>(BSLjava/nio/ByteBuffer;)V

    .line 111
    sget-object v10, Lf/p22;->lPT2:Lf/p22;

    .line 112
    invoke-virtual {v10, v8, v0, v11}, Lf/p22;->i8(BSLf/ev5;)V

    sget-boolean v0, Lf/ms5;->T20:Z

    if-eqz v0, :cond_cc2

    invoke-virtual {v6}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v0

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v8

    invoke-interface {v9, v5, v0, v8}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_cc2

    :catch_cbc
    const-string v1, "{} has an invalid footer id."

    invoke-interface {v9, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_cc2
    :goto_cc2
    array-length v0, v2

    const/4 v8, 0x2

    if-ne v0, v8, :cond_d26

    const-string v0, "world_map_headers"

    const/16 v22, 0x0

    aget-object v8, v2, v22

    invoke-virtual {v0, v8}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_d26

    const/16 v18, 0x1

    aget-object v0, v2, v18

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v7

    if-nez v7, :cond_ce4

    invoke-interface {v9, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_ce4
    const-string v1, "\\."

    invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    const/16 v22, 0x0

    :try_start_cec
    aget-object v7, v1, v22

    invoke-static {v7}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v7

    const/16 v18, 0x1

    aget-object v1, v1, v18

    invoke-static {v1}, Ljava/lang/Byte;->parseByte(Ljava/lang/String;)B

    move-result v0
    :try_end_cfa
    .catch Ljava/lang/Exception; {:try_start_cec .. :try_end_cfa} :catch_d22

    invoke-virtual {v6}, Lf/z46;->yD()[B

    move-result-object v1

    invoke-static {v1}, Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;

    move-result-object v1

    sget-object v3, Ljava/nio/ByteOrder;->LITTLE_ENDIAN:Ljava/nio/ByteOrder;

    invoke-virtual {v1, v3}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    move-result-object v1

    new-instance v3, Lf/q05;

    invoke-direct {v3, v1, v7, v0}, Lf/q05;-><init>(Ljava/nio/ByteBuffer;II)V

    .line 113
    sget-object v1, Lf/p22;->lPT2:Lf/p22;

    .line 114
    invoke-virtual {v1, v7, v0, v3}, Lf/p22;->I50(BBLf/q05;)V

    sget-boolean v0, Lf/ms5;->T20:Z

    if-eqz v0, :cond_d26

    invoke-virtual {v6}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v0

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v1

    invoke-interface {v9, v5, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_d26

    :catch_d22
    invoke-interface {v9, v3, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_d26
    :goto_d26
    array-length v0, v2

    const/4 v8, 0x2

    if-ne v0, v8, :cond_d7a

    const-string v0, "maps"

    const/16 v22, 0x0

    aget-object v1, v2, v22

    invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_d7a

    const/16 v18, 0x1

    aget-object v0, v2, v18

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    const-string v1, ".tmx"

    invoke-virtual {v0, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-nez v1, :cond_d4c

    const-string v0, "Only .tmx files supported for /maps/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_d4c
    const/16 v14, 0x2e

    :try_start_d4e
    invoke-virtual {v0, v14}, Ljava/lang/String;->indexOf(I)I

    move-result v1

    const/4 v8, 0x0

    invoke-virtual {v0, v8, v1}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0
    :try_end_d5b
    .catch Ljava/lang/Exception; {:try_start_d4e .. :try_end_d5b} :catch_d74

    invoke-static {}, Lf/dh4;->M40()Lf/dh4;

    move-result-object v1

    iget-object v3, v15, Lf/c85;->TE:Lf/im1;

    invoke-virtual {v1, v0, v6, v3}, Lf/dh4;->kG1(SLf/z46;Lf/im1;)V

    sget-boolean v0, Lf/ms5;->T20:Z

    if-eqz v0, :cond_d7a

    invoke-virtual {v6}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v0

    invoke-virtual/range {v20 .. v20}, Lf/z46;->yJ()Ljava/lang/String;

    move-result-object v1

    invoke-interface {v9, v5, v0, v1}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    goto :goto_d7a

    :catch_d74
    const-string v1, "{} has an invalid map_footer_connectionlist_id."

    invoke-interface {v9, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    return-void

    :cond_d7a
    :goto_d7a
    array-length v0, v2

    const/4 v8, 0x2

    if-ne v0, v8, :cond_dc3

    const-string v0, "costumes"

    const/16 v22, 0x0

    aget-object v1, v2, v22

    invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_dc3

    const/16 v18, 0x1

    aget-object v0, v2, v18

    invoke-virtual {v0, v4}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;

    move-result-object v0

    const-string v1, ".costume"

    invoke-virtual {v0, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-nez v1, :cond_da0

    const-string v0, "Only .costume files supported for /costumes/"

    invoke-interface {v9, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    return-void

    :cond_da0
    :try_start_da0
    const-string v1, "[.-]"

    invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    const/16 v22, 0x0

    aget-object v2, v1, v22

    invoke-static {v2}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v2

    const/16 v18, 0x1

    aget-object v1, v1, v18

    invoke-static {v1}, Ljava/lang/Short;->parseShort(Ljava/lang/String;)S

    move-result v0
    :try_end_db6
    .catch Ljava/lang/NumberFormatException; {:try_start_da0 .. :try_end_db6} :catch_dbe

    invoke-static {}, Lf/lw;->oZ()Lf/lw;

    move-result-object v1

    invoke-virtual {v1, v2, v0, v6}, Lf/lw;->iA0(SSLf/z46;)V

    return-void

    :catch_dbe
    const-string v1, "{} is not a valid format for costume files. Must use spriteId-baseSpriteId.costume formatting."

    invoke-interface {v9, v1, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    :cond_dc3
    :goto_dc3
    return-void
.end method

.method public final cE1(Lorg/w3c/dom/Element;Ljava/lang/String;)Z
    .registers 28

    .line 1
    move-object/from16 v0, p1

    .line 2
    .line 3
    move-object/from16 v1, p2

    .line 4
    .line 5
    const-string v2, "constants"

    .line 6
    .line 7
    invoke-interface {v0, v2}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 8
    .line 9
    .line 10
    move-result-object v3

    .line 11
    invoke-interface {v3}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 12
    .line 13
    .line 14
    move-result v3

    .line 15
    const/4 v4, 0x1

    .line 16
    move-object/from16 v5, p0

    .line 17
    .line 18
    iget-object v6, v5, Lf/c85;->BP1:Lf/z46;

    .line 19
    .line 20
    sget-object v7, Lf/c85;->jL:Lf/xv7;

    .line 21
    .line 22
    const/4 v8, 0x0

    .line 23
    if-gt v3, v4, :cond_3da

    .line 24
    .line 25
    const-string v3, "constants_presets"

    .line 26
    .line 27
    invoke-interface {v0, v3}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 28
    .line 29
    .line 30
    move-result-object v9

    .line 31
    invoke-interface {v9}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 32
    .line 33
    .line 34
    move-result v9

    .line 35
    if-gt v9, v4, :cond_3cc

    .line 36
    .line 37
    invoke-static {v0, v2}, Lf/c85;->QH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 38
    .line 39
    .line 40
    move-result-object v2

    .line 41
    invoke-static {v0, v3}, Lf/c85;->QH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 42
    .line 43
    .line 44
    move-result-object v0

    .line 45
    new-instance v3, Ljava/util/HashMap;

    .line 46
    .line 47
    invoke-direct {v3}, Ljava/util/HashMap;-><init>()V

    .line 48
    .line 49
    .line 50
    const-string v9, "constant"

    .line 51
    .line 52
    invoke-interface {v2, v9}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 53
    .line 54
    .line 55
    move-result-object v2

    .line 56
    const/4 v10, 0x0

    .line 57
    :goto_38
    invoke-interface {v2}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 58
    .line 59
    .line 60
    move-result v11

    .line 61
    const/4 v12, 0x4

    .line 62
    const/4 v13, 0x2

    .line 63
    const/4 v14, 0x3

    .line 64
    const/4 v15, 0x0

    .line 65
    const/16 v16, 0x1

    .line 66
    .line 67
    const-string v4, "name"

    .line 68
    .line 69
    if-ge v10, v11, :cond_276

    .line 70
    .line 71
    invoke-interface {v2, v10}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 72
    .line 73
    .line 74
    move-result-object v11

    .line 75
    check-cast v11, Lorg/w3c/dom/Element;

    .line 76
    .line 77
    invoke-interface {v11, v4}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 78
    .line 79
    .line 80
    move-result v17

    .line 81
    if-nez v17, :cond_5e

    .line 82
    .line 83
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 84
    .line 85
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 86
    .line 87
    .line 88
    move-result-object v0

    .line 89
    const-string v2, "Constant in theme {} has no name attribute: {}"

    .line 90
    .line 91
    invoke-interface {v7, v2, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 92
    .line 93
    .line 94
    return v8

    .line 95
    :cond_5e
    invoke-interface {v11, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 96
    .line 97
    .line 98
    move-result-object v4

    .line 99
    invoke-virtual {v3, v4}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    .line 100
    .line 101
    .line 102
    move-result v17

    .line 103
    if-eqz v17, :cond_7c

    .line 104
    .line 105
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 106
    .line 107
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 108
    .line 109
    .line 110
    move-result-object v0

    .line 111
    new-array v2, v14, [Ljava/lang/Object;

    .line 112
    .line 113
    aput-object v4, v2, v8

    .line 114
    .line 115
    aput-object v1, v2, v16

    .line 116
    .line 117
    aput-object v0, v2, v13

    .line 118
    .line 119
    const-string v0, "Constant {} in theme {} is duplicate: {}"

    .line 120
    .line 121
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 122
    .line 123
    .line 124
    return v8

    .line 125
    :cond_7c
    const/16 p1, 0x2

    .line 126
    .line 127
    const-string v13, "type"

    .line 128
    .line 129
    invoke-interface {v11, v13}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 130
    .line 131
    .line 132
    move-result v17

    .line 133
    if-nez v17, :cond_9a

    .line 134
    .line 135
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 136
    .line 137
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 138
    .line 139
    .line 140
    move-result-object v0

    .line 141
    new-array v2, v14, [Ljava/lang/Object;

    .line 142
    .line 143
    aput-object v4, v2, v8

    .line 144
    .line 145
    aput-object v1, v2, v16

    .line 146
    .line 147
    aput-object v0, v2, p1

    .line 148
    .line 149
    const-string v0, "Constant {} in theme {} has no type attribute: {}"

    .line 150
    .line 151
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 152
    .line 153
    .line 154
    return v8

    .line 155
    :cond_9a
    invoke-interface {v11, v13}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 156
    .line 157
    .line 158
    move-result-object v13

    .line 159
    invoke-virtual {v13}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    .line 160
    .line 161
    .line 162
    move-result-object v13

    .line 163
    :try_start_a2
    invoke-static {v13}, Lf/vt3;->valueOf(Ljava/lang/String;)Lf/vt3;
    :try_end_a5
    .catch Ljava/lang/IllegalArgumentException; {:try_start_a2 .. :try_end_a5} :catch_259

    .line 164
    .line 165
    .line 166
    invoke-static {v13}, Lf/vt3;->valueOf(Ljava/lang/String;)Lf/vt3;

    .line 167
    .line 168
    .line 169
    move-result-object v13

    .line 170
    const/16 v17, 0x0

    .line 171
    .line 172
    const-string v8, "default_value"

    .line 173
    .line 174
    invoke-interface {v11, v8}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 175
    .line 176
    .line 177
    move-result v18

    .line 178
    if-nez v18, :cond_c7

    .line 179
    .line 180
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 181
    .line 182
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 183
    .line 184
    .line 185
    move-result-object v0

    .line 186
    new-array v2, v14, [Ljava/lang/Object;

    .line 187
    .line 188
    aput-object v4, v2, v17

    .line 189
    .line 190
    aput-object v1, v2, v16

    .line 191
    .line 192
    aput-object v0, v2, p1

    .line 193
    .line 194
    const-string v0, "Constant {} in theme {} has no default_value attribute: {}"

    .line 195
    .line 196
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 197
    .line 198
    .line 199
    return v17

    .line 200
    :cond_c7
    invoke-interface {v11, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 201
    .line 202
    .line 203
    move-result-object v8

    .line 204
    invoke-virtual {v13}, Lf/vt3;->uI()Z

    .line 205
    .line 206
    .line 207
    move-result v18

    .line 208
    if-eqz v18, :cond_ed

    .line 209
    .line 210
    invoke-virtual {v13, v15, v8}, Lf/vt3;->r21(Lf/sz4;Ljava/lang/String;)Ljava/lang/Object;

    .line 211
    .line 212
    .line 213
    move-result-object v18

    .line 214
    if-nez v18, :cond_ed

    .line 215
    .line 216
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 217
    .line 218
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 219
    .line 220
    .line 221
    move-result-object v0

    .line 222
    new-array v2, v12, [Ljava/lang/Object;

    .line 223
    .line 224
    aput-object v4, v2, v17

    .line 225
    .line 226
    aput-object v1, v2, v16

    .line 227
    .line 228
    aput-object v8, v2, p1

    .line 229
    .line 230
    aput-object v0, v2, v14

    .line 231
    .line 232
    const-string v0, "Constant {} in theme {} has default_value {} but it can\'t be parsed: {}"

    .line 233
    .line 234
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 235
    .line 236
    .line 237
    return v17

    .line 238
    :cond_ed
    invoke-virtual {v3, v4, v13}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 239
    .line 240
    .line 241
    const-string v15, "whitelist_value"

    .line 242
    .line 243
    invoke-interface {v11, v15}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 244
    .line 245
    .line 246
    move-result-object v12

    .line 247
    invoke-interface {v12}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 248
    .line 249
    .line 250
    move-result v20

    .line 251
    if-nez v20, :cond_1a7

    .line 252
    .line 253
    sget-object v12, Lf/vt3;->bH:Lf/vt3;

    .line 254
    .line 255
    if-ne v13, v12, :cond_114

    .line 256
    .line 257
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 258
    .line 259
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 260
    .line 261
    .line 262
    move-result-object v0

    .line 263
    new-array v2, v14, [Ljava/lang/Object;

    .line 264
    .line 265
    aput-object v4, v2, v17

    .line 266
    .line 267
    aput-object v1, v2, v16

    .line 268
    .line 269
    aput-object v0, v2, p1

    .line 270
    .line 271
    const-string v0, "Constant {} in theme {} has no whitelist_values defined but is of type constant, which requires it: {}"

    .line 272
    .line 273
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 274
    .line 275
    .line 276
    return v17

    .line 277
    :cond_114
    sget-object v12, Lf/vt3;->Jn0:Lf/vt3;

    .line 278
    .line 279
    if-ne v13, v12, :cond_13b

    .line 280
    .line 281
    invoke-static {}, Lf/b63;->values()[Lf/b63;

    .line 282
    .line 283
    .line 284
    move-result-object v12

    .line 285
    invoke-static {v12}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 286
    .line 287
    .line 288
    move-result-object v12

    .line 289
    new-instance v14, Lf/oc7;

    .line 290
    .line 291
    move-object/from16 v21, v2

    .line 292
    .line 293
    const/16 v2, 0x18

    .line 294
    .line 295
    invoke-direct {v14, v2}, Lf/oc7;-><init>(I)V

    .line 296
    .line 297
    .line 298
    invoke-interface {v12, v14}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 299
    .line 300
    .line 301
    move-result-object v2

    .line 302
    new-instance v12, Lf/dh0;

    .line 303
    .line 304
    const/16 v14, 0xf

    .line 305
    .line 306
    invoke-direct {v12, v14}, Lf/dh0;-><init>(I)V

    .line 307
    .line 308
    .line 309
    :goto_134
    invoke-interface {v2, v12}, Lj$/util/stream/Stream;->toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;

    .line 310
    .line 311
    .line 312
    move-result-object v2

    .line 313
    check-cast v2, [Ljava/lang/String;

    .line 314
    .line 315
    goto :goto_17c

    .line 316
    :cond_13b
    move-object/from16 v21, v2

    .line 317
    .line 318
    sget-object v2, Lf/vt3;->la:Lf/vt3;

    .line 319
    .line 320
    if-ne v13, v2, :cond_15c

    .line 321
    .line 322
    invoke-static {}, Lf/w58;->values()[Lf/w58;

    .line 323
    .line 324
    .line 325
    move-result-object v2

    .line 326
    invoke-static {v2}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 327
    .line 328
    .line 329
    move-result-object v2

    .line 330
    new-instance v12, Lf/oc7;

    .line 331
    .line 332
    const/16 v14, 0x19

    .line 333
    .line 334
    invoke-direct {v12, v14}, Lf/oc7;-><init>(I)V

    .line 335
    .line 336
    .line 337
    invoke-interface {v2, v12}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 338
    .line 339
    .line 340
    move-result-object v2

    .line 341
    new-instance v12, Lf/dh0;

    .line 342
    .line 343
    const/16 v14, 0x10

    .line 344
    .line 345
    invoke-direct {v12, v14}, Lf/dh0;-><init>(I)V

    .line 346
    .line 347
    .line 348
    goto :goto_134

    .line 349
    :cond_15c
    sget-object v2, Lf/vt3;->Pn0:Lf/vt3;

    .line 350
    .line 351
    if-ne v13, v2, :cond_17b

    .line 352
    .line 353
    invoke-static {}, Lf/ws6;->values()[Lf/ws6;

    .line 354
    .line 355
    .line 356
    move-result-object v2

    .line 357
    invoke-static {v2}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 358
    .line 359
    .line 360
    move-result-object v2

    .line 361
    new-instance v12, Lf/oc7;

    .line 362
    .line 363
    const/16 v14, 0x1a

    .line 364
    .line 365
    invoke-direct {v12, v14}, Lf/oc7;-><init>(I)V

    .line 366
    .line 367
    .line 368
    invoke-interface {v2, v12}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 369
    .line 370
    .line 371
    move-result-object v2

    .line 372
    new-instance v12, Lf/dh0;

    .line 373
    .line 374
    const/16 v14, 0x11

    .line 375
    .line 376
    invoke-direct {v12, v14}, Lf/dh0;-><init>(I)V

    .line 377
    .line 378
    .line 379
    goto :goto_134

    .line 380
    :cond_17b
    const/4 v2, 0x0

    .line 381
    :goto_17c
    if-eqz v2, :cond_1a0

    .line 382
    .line 383
    invoke-interface {v11}, Lorg/w3c/dom/Node;->getOwnerDocument()Lorg/w3c/dom/Document;

    .line 384
    .line 385
    .line 386
    move-result-object v12

    .line 387
    array-length v14, v2

    .line 388
    move-object/from16 v22, v2

    .line 389
    .line 390
    const/4 v2, 0x0

    .line 391
    :goto_186
    if-ge v2, v14, :cond_19d

    .line 392
    .line 393
    move/from16 v23, v2

    .line 394
    .line 395
    aget-object v2, v22, v23

    .line 396
    .line 397
    move-object/from16 v24, v4

    .line 398
    .line 399
    invoke-interface {v12, v15}, Lorg/w3c/dom/Document;->createElement(Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 400
    .line 401
    .line 402
    move-result-object v4

    .line 403
    invoke-interface {v4, v2}, Lorg/w3c/dom/Node;->setTextContent(Ljava/lang/String;)V

    .line 404
    .line 405
    .line 406
    invoke-interface {v11, v4}, Lorg/w3c/dom/Node;->appendChild(Lorg/w3c/dom/Node;)Lorg/w3c/dom/Node;

    .line 407
    .line 408
    .line 409
    add-int/lit8 v2, v23, 0x1

    .line 410
    .line 411
    move-object/from16 v4, v24

    .line 412
    .line 413
    goto :goto_186

    .line 414
    :cond_19d
    :goto_19d
    move-object/from16 v24, v4

    .line 415
    .line 416
    goto :goto_1a3

    .line 417
    :cond_1a0
    move-object/from16 v22, v2

    .line 418
    .line 419
    goto :goto_19d

    .line 420
    :goto_1a3
    move-object/from16 v2, v22

    .line 421
    .line 422
    const/4 v4, 0x0

    .line 423
    goto :goto_1ce

    .line 424
    :cond_1a7
    move-object/from16 v21, v2

    .line 425
    .line 426
    move-object/from16 v24, v4

    .line 427
    .line 428
    new-instance v2, Ljava/util/ArrayList;

    .line 429
    .line 430
    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    .line 431
    .line 432
    .line 433
    const/4 v4, 0x0

    .line 434
    :goto_1b1
    invoke-interface {v12}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 435
    .line 436
    .line 437
    move-result v11

    .line 438
    if-ge v4, v11, :cond_1c5

    .line 439
    .line 440
    invoke-interface {v12, v4}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 441
    .line 442
    .line 443
    move-result-object v11

    .line 444
    invoke-interface {v11}, Lorg/w3c/dom/Node;->getTextContent()Ljava/lang/String;

    .line 445
    .line 446
    .line 447
    move-result-object v11

    .line 448
    invoke-virtual {v2, v11}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 449
    .line 450
    .line 451
    add-int/lit8 v4, v4, 0x1

    .line 452
    .line 453
    goto :goto_1b1

    .line 454
    :cond_1c5
    const/4 v4, 0x0

    .line 455
    new-array v11, v4, [Ljava/lang/String;

    .line 456
    .line 457
    invoke-virtual {v2, v11}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 458
    .line 459
    .line 460
    move-result-object v2

    .line 461
    check-cast v2, [Ljava/lang/String;

    .line 462
    .line 463
    :goto_1ce
    if-eqz v2, :cond_251

    .line 464
    .line 465
    array-length v11, v2

    .line 466
    if-nez v11, :cond_1e8

    .line 467
    .line 468
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 469
    .line 470
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 471
    .line 472
    .line 473
    move-result-object v0

    .line 474
    const/4 v11, 0x3

    .line 475
    new-array v2, v11, [Ljava/lang/Object;

    .line 476
    .line 477
    aput-object v24, v2, v4

    .line 478
    .line 479
    aput-object v1, v2, v16

    .line 480
    .line 481
    aput-object v0, v2, p1

    .line 482
    .line 483
    const-string v0, "Constant {} in theme {} has whitelist_values defined but its empty: {}"

    .line 484
    .line 485
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 486
    .line 487
    .line 488
    return v4

    .line 489
    :cond_1e8
    const/4 v11, 0x3

    .line 490
    sget-object v12, Lf/vt3;->YW1:Lf/vt3;

    .line 491
    .line 492
    if-ne v13, v12, :cond_201

    .line 493
    .line 494
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 495
    .line 496
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 497
    .line 498
    .line 499
    move-result-object v0

    .line 500
    new-array v2, v11, [Ljava/lang/Object;

    .line 501
    .line 502
    aput-object v24, v2, v4

    .line 503
    .line 504
    aput-object v1, v2, v16

    .line 505
    .line 506
    aput-object v0, v2, p1

    .line 507
    .line 508
    const-string v0, "Constant {} in theme {} has whitelist_values defined but is of type bool, which is incompatible: {}"

    .line 509
    .line 510
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 511
    .line 512
    .line 513
    return v4

    .line 514
    :cond_201
    invoke-static {v8, v2}, Lf/qy4;->cH1(Ljava/lang/Object;[Ljava/lang/Object;)Z

    .line 515
    .line 516
    .line 517
    move-result v11

    .line 518
    if-nez v11, :cond_220

    .line 519
    .line 520
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 521
    .line 522
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 523
    .line 524
    .line 525
    move-result-object v0

    .line 526
    const/4 v2, 0x4

    .line 527
    new-array v2, v2, [Ljava/lang/Object;

    .line 528
    .line 529
    aput-object v24, v2, v4

    .line 530
    .line 531
    aput-object v1, v2, v16

    .line 532
    .line 533
    aput-object v8, v2, p1

    .line 534
    .line 535
    const/16 v20, 0x3

    .line 536
    .line 537
    aput-object v0, v2, v20

    .line 538
    .line 539
    const-string v0, "Constant {} in theme {} has default_value {} but its not contained in the whitelist: {}"

    .line 540
    .line 541
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 542
    .line 543
    .line 544
    return v4

    .line 545
    :cond_220
    invoke-virtual {v13}, Lf/vt3;->uI()Z

    .line 546
    .line 547
    .line 548
    move-result v4

    .line 549
    if-eqz v4, :cond_251

    .line 550
    .line 551
    array-length v4, v2

    .line 552
    const/4 v8, 0x0

    .line 553
    :goto_228
    if-ge v8, v4, :cond_251

    .line 554
    .line 555
    aget-object v11, v2, v8

    .line 556
    .line 557
    const/4 v12, 0x0

    .line 558
    invoke-virtual {v13, v12, v11}, Lf/vt3;->r21(Lf/sz4;Ljava/lang/String;)Ljava/lang/Object;

    .line 559
    .line 560
    .line 561
    move-result-object v14

    .line 562
    if-nez v14, :cond_24e

    .line 563
    .line 564
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 565
    .line 566
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 567
    .line 568
    .line 569
    move-result-object v0

    .line 570
    const/4 v2, 0x4

    .line 571
    new-array v2, v2, [Ljava/lang/Object;

    .line 572
    .line 573
    const/16 v17, 0x0

    .line 574
    .line 575
    aput-object v24, v2, v17

    .line 576
    .line 577
    aput-object v1, v2, v16

    .line 578
    .line 579
    aput-object v11, v2, p1

    .line 580
    .line 581
    const/16 v20, 0x3

    .line 582
    .line 583
    aput-object v0, v2, v20

    .line 584
    .line 585
    const-string v0, "Constant {} in theme {} has whitelist_value {} but it can\'t be parsed: {}"

    .line 586
    .line 587
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 588
    .line 589
    .line 590
    return v17

    .line 591
    :cond_24e
    add-int/lit8 v8, v8, 0x1

    .line 592
    .line 593
    goto :goto_228

    .line 594
    :cond_251
    add-int/lit8 v10, v10, 0x1

    .line 595
    .line 596
    move-object/from16 v2, v21

    .line 597
    .line 598
    const/4 v4, 0x1

    .line 599
    const/4 v8, 0x0

    .line 600
    goto/16 :goto_38

    .line 601
    .line 602
    :catch_259
    move-object/from16 v24, v4

    .line 603
    .line 604
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 605
    .line 606
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 607
    .line 608
    .line 609
    move-result-object v0

    .line 610
    const/4 v2, 0x4

    .line 611
    new-array v2, v2, [Ljava/lang/Object;

    .line 612
    .line 613
    const/16 v17, 0x0

    .line 614
    .line 615
    aput-object v24, v2, v17

    .line 616
    .line 617
    aput-object v1, v2, v16

    .line 618
    .line 619
    aput-object v13, v2, p1

    .line 620
    .line 621
    const/16 v20, 0x3

    .line 622
    .line 623
    aput-object v0, v2, v20

    .line 624
    .line 625
    const-string v0, "Constant {} in theme {} has unknown type {}: {}"

    .line 626
    .line 627
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 628
    .line 629
    .line 630
    return v17

    .line 631
    :cond_276
    const/16 p1, 0x2

    .line 632
    .line 633
    new-instance v2, Ljava/util/HashSet;

    .line 634
    .line 635
    invoke-direct {v2}, Ljava/util/HashSet;-><init>()V

    .line 636
    .line 637
    .line 638
    const-string v8, "constants_preset"

    .line 639
    .line 640
    invoke-interface {v0, v8}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 641
    .line 642
    .line 643
    move-result-object v0

    .line 644
    const/4 v8, 0x0

    .line 645
    :goto_284
    invoke-interface {v0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 646
    .line 647
    .line 648
    move-result v10

    .line 649
    if-ge v8, v10, :cond_3cb

    .line 650
    .line 651
    invoke-interface {v0, v8}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 652
    .line 653
    .line 654
    move-result-object v10

    .line 655
    check-cast v10, Lorg/w3c/dom/Element;

    .line 656
    .line 657
    invoke-interface {v10, v4}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 658
    .line 659
    .line 660
    move-result v11

    .line 661
    if-nez v11, :cond_2ad

    .line 662
    .line 663
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 664
    .line 665
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 666
    .line 667
    .line 668
    move-result-object v2

    .line 669
    const/4 v11, 0x3

    .line 670
    new-array v3, v11, [Ljava/lang/Object;

    .line 671
    .line 672
    const/16 v17, 0x0

    .line 673
    .line 674
    aput-object v0, v3, v17

    .line 675
    .line 676
    aput-object v1, v3, v16

    .line 677
    .line 678
    aput-object v2, v3, p1

    .line 679
    .line 680
    const-string v0, "Constant-Preset {} in theme {} has no name: {}"

    .line 681
    .line 682
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 683
    .line 684
    .line 685
    return v17

    .line 686
    :cond_2ad
    const/16 v17, 0x0

    .line 687
    .line 688
    invoke-interface {v10, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 689
    .line 690
    .line 691
    move-result-object v11

    .line 692
    invoke-virtual {v2, v11}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 693
    .line 694
    .line 695
    move-result v11

    .line 696
    if-nez v11, :cond_2ce

    .line 697
    .line 698
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 699
    .line 700
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 701
    .line 702
    .line 703
    move-result-object v2

    .line 704
    const/4 v11, 0x3

    .line 705
    new-array v3, v11, [Ljava/lang/Object;

    .line 706
    .line 707
    aput-object v0, v3, v17

    .line 708
    .line 709
    aput-object v1, v3, v16

    .line 710
    .line 711
    aput-object v2, v3, p1

    .line 712
    .line 713
    const-string v0, "Constant-Preset {} in theme {} exist multiple times: {}"

    .line 714
    .line 715
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 716
    .line 717
    .line 718
    return v17

    .line 719
    :cond_2ce
    new-instance v11, Ljava/util/HashSet;

    .line 720
    .line 721
    invoke-virtual {v3}, Ljava/util/HashMap;->keySet()Ljava/util/Set;

    .line 722
    .line 723
    .line 724
    move-result-object v12

    .line 725
    invoke-direct {v11, v12}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V

    .line 726
    .line 727
    .line 728
    invoke-interface {v10, v9}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 729
    .line 730
    .line 731
    move-result-object v10

    .line 732
    const/4 v12, 0x0

    .line 733
    :goto_2dc
    invoke-interface {v10}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 734
    .line 735
    .line 736
    move-result v13

    .line 737
    if-ge v12, v13, :cond_388

    .line 738
    .line 739
    invoke-interface {v10, v12}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 740
    .line 741
    .line 742
    move-result-object v13

    .line 743
    check-cast v13, Lorg/w3c/dom/Element;

    .line 744
    .line 745
    invoke-interface {v13, v4}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 746
    .line 747
    .line 748
    move-result v14

    .line 749
    if-nez v14, :cond_305

    .line 750
    .line 751
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 752
    .line 753
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 754
    .line 755
    .line 756
    move-result-object v2

    .line 757
    const/4 v11, 0x3

    .line 758
    new-array v3, v11, [Ljava/lang/Object;

    .line 759
    .line 760
    const/16 v17, 0x0

    .line 761
    .line 762
    aput-object v0, v3, v17

    .line 763
    .line 764
    aput-object v1, v3, v16

    .line 765
    .line 766
    aput-object v2, v3, p1

    .line 767
    .line 768
    const-string v0, "Constant-Preset {} in theme {} has a constant with no name : {}"

    .line 769
    .line 770
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 771
    .line 772
    .line 773
    return v17

    .line 774
    :cond_305
    const/16 v17, 0x0

    .line 775
    .line 776
    invoke-interface {v13, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 777
    .line 778
    .line 779
    move-result-object v14

    .line 780
    invoke-interface {v13}, Lorg/w3c/dom/Node;->getTextContent()Ljava/lang/String;

    .line 781
    .line 782
    .line 783
    move-result-object v13

    .line 784
    invoke-virtual {v3, v14}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z

    .line 785
    .line 786
    .line 787
    move-result v15

    .line 788
    if-nez v15, :cond_32e

    .line 789
    .line 790
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 791
    .line 792
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 793
    .line 794
    .line 795
    move-result-object v2

    .line 796
    const/4 v3, 0x4

    .line 797
    new-array v3, v3, [Ljava/lang/Object;

    .line 798
    .line 799
    aput-object v0, v3, v17

    .line 800
    .line 801
    aput-object v1, v3, v16

    .line 802
    .line 803
    aput-object v14, v3, p1

    .line 804
    .line 805
    const/16 v20, 0x3

    .line 806
    .line 807
    aput-object v2, v3, v20

    .line 808
    .line 809
    const-string v0, "Constant-Preset {} in theme {} references constant {} but it was never declared: {}"

    .line 810
    .line 811
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 812
    .line 813
    .line 814
    return v17

    .line 815
    :cond_32e
    invoke-virtual {v11, v14}, Ljava/util/HashSet;->remove(Ljava/lang/Object;)Z

    .line 816
    .line 817
    .line 818
    move-result v15

    .line 819
    if-nez v15, :cond_34d

    .line 820
    .line 821
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 822
    .line 823
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 824
    .line 825
    .line 826
    move-result-object v2

    .line 827
    const/4 v3, 0x4

    .line 828
    new-array v3, v3, [Ljava/lang/Object;

    .line 829
    .line 830
    aput-object v0, v3, v17

    .line 831
    .line 832
    aput-object v1, v3, v16

    .line 833
    .line 834
    aput-object v14, v3, p1

    .line 835
    .line 836
    const/16 v20, 0x3

    .line 837
    .line 838
    aput-object v2, v3, v20

    .line 839
    .line 840
    const-string v0, "Constant-Preset {} in theme {} references constant {} multiple times: {}"

    .line 841
    .line 842
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 843
    .line 844
    .line 845
    return v17

    .line 846
    :cond_34d
    invoke-virtual {v3, v14}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 847
    .line 848
    .line 849
    move-result-object v15

    .line 850
    check-cast v15, Lf/vt3;

    .line 851
    .line 852
    invoke-virtual {v15}, Lf/vt3;->uI()Z

    .line 853
    .line 854
    .line 855
    move-result v21

    .line 856
    if-eqz v21, :cond_37f

    .line 857
    .line 858
    move-object/from16 v21, v0

    .line 859
    .line 860
    const/4 v0, 0x0

    .line 861
    invoke-virtual {v15, v0, v13}, Lf/vt3;->r21(Lf/sz4;Ljava/lang/String;)Ljava/lang/Object;

    .line 862
    .line 863
    .line 864
    move-result-object v15

    .line 865
    if-nez v15, :cond_382

    .line 866
    .line 867
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 868
    .line 869
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 870
    .line 871
    .line 872
    move-result-object v0

    .line 873
    const/4 v2, 0x5

    .line 874
    new-array v2, v2, [Ljava/lang/Object;

    .line 875
    .line 876
    aput-object v21, v2, v17

    .line 877
    .line 878
    aput-object v1, v2, v16

    .line 879
    .line 880
    aput-object v14, v2, p1

    .line 881
    .line 882
    const/16 v20, 0x3

    .line 883
    .line 884
    aput-object v13, v2, v20

    .line 885
    .line 886
    const/16 v19, 0x4

    .line 887
    .line 888
    aput-object v0, v2, v19

    .line 889
    .line 890
    const-string v0, "Constant-Preset {} in theme {} references constant {} with value {} but it can\'t be parsed: {}"

    .line 891
    .line 892
    invoke-interface {v7, v0, v2}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 893
    .line 894
    .line 895
    return v17

    .line 896
    :cond_37f
    move-object/from16 v21, v0

    .line 897
    .line 898
    const/4 v0, 0x0

    .line 899
    :cond_382
    add-int/lit8 v12, v12, 0x1

    .line 900
    .line 901
    move-object/from16 v0, v21

    .line 902
    .line 903
    goto/16 :goto_2dc

    .line 904
    .line 905
    :cond_388
    move-object/from16 v21, v0

    .line 906
    .line 907
    const/4 v0, 0x0

    .line 908
    invoke-virtual {v11}, Ljava/util/HashSet;->isEmpty()Z

    .line 909
    .line 910
    .line 911
    move-result v10

    .line 912
    if-nez v10, :cond_3c0

    .line 913
    .line 914
    invoke-static {v11}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 915
    .line 916
    .line 917
    move-result-object v0

    .line 918
    invoke-interface {v0}, Lj$/util/stream/Stream;->sorted()Lj$/util/stream/Stream;

    .line 919
    .line 920
    .line 921
    move-result-object v0

    .line 922
    const-string v2, ", "

    .line 923
    .line 924
    invoke-static {v2}, Lj$/util/stream/Collectors;->joining(Ljava/lang/CharSequence;)Lj$/util/stream/Collector;

    .line 925
    .line 926
    .line 927
    move-result-object v2

    .line 928
    invoke-interface {v0, v2}, Lj$/util/stream/Stream;->collect(Lj$/util/stream/Collector;)Ljava/lang/Object;

    .line 929
    .line 930
    .line 931
    move-result-object v0

    .line 932
    check-cast v0, Ljava/lang/String;

    .line 933
    .line 934
    iget-object v2, v6, Lf/z46;->O01:Ljava/io/File;

    .line 935
    .line 936
    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 937
    .line 938
    .line 939
    move-result-object v2

    .line 940
    const/4 v10, 0x4

    .line 941
    new-array v3, v10, [Ljava/lang/Object;

    .line 942
    .line 943
    const/16 v17, 0x0

    .line 944
    .line 945
    aput-object v21, v3, v17

    .line 946
    .line 947
    aput-object v1, v3, v16

    .line 948
    .line 949
    aput-object v0, v3, p1

    .line 950
    .line 951
    const/16 v20, 0x3

    .line 952
    .line 953
    aput-object v2, v3, v20

    .line 954
    .line 955
    const-string v0, "Constant-Preset {} in theme {} does not define values for the constants {}: {}"

    .line 956
    .line 957
    invoke-interface {v7, v0, v3}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 958
    .line 959
    .line 960
    return v17

    .line 961
    :cond_3c0
    const/4 v10, 0x4

    .line 962
    const/16 v17, 0x0

    .line 963
    .line 964
    const/16 v20, 0x3

    .line 965
    .line 966
    add-int/lit8 v8, v8, 0x1

    .line 967
    .line 968
    move-object/from16 v0, v21

    .line 969
    .line 970
    goto/16 :goto_284

    .line 971
    .line 972
    :cond_3cb
    return v16

    .line 973
    :cond_3cc
    const/16 v17, 0x0

    .line 974
    .line 975
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 976
    .line 977
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 978
    .line 979
    .line 980
    move-result-object v0

    .line 981
    const-string v2, "`constants_presets` block in theme {} should only exist once: {}"

    .line 982
    .line 983
    invoke-interface {v7, v2, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 984
    .line 985
    .line 986
    return v17

    .line 987
    :cond_3da
    const/16 v17, 0x0

    .line 988
    .line 989
    iget-object v0, v6, Lf/z46;->O01:Ljava/io/File;

    .line 990
    .line 991
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 992
    .line 993
    .line 994
    move-result-object v0

    .line 995
    const-string v2, "`constants` block in theme {} should only exist once: {}"

    .line 996
    .line 997
    invoke-interface {v7, v2, v1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 998
    .line 999
    .line 1000
    return v17
.end method

.method public final co1(Ljava/io/InputStream;)Z
    .registers 19

    .line 1
    move-object/from16 v1, p0

    .line 2
    .line 3
    sget-object v2, Lf/c85;->jL:Lf/xv7;

    .line 4
    .line 5
    const-string v0, "weblink"

    .line 6
    .line 7
    const-string v3, "author"

    .line 8
    .line 9
    const-string v4, "description"

    .line 10
    .line 11
    const-string v5, "version"

    .line 12
    .line 13
    const-string v6, "name"

    .line 14
    .line 15
    iget-object v7, v1, Lf/c85;->BP1:Lf/z46;

    .line 16
    .line 17
    const/4 v8, 0x0

    .line 18
    :try_start_11
    invoke-static {}, Ljavax/xml/parsers/DocumentBuilderFactory;->newInstance()Ljavax/xml/parsers/DocumentBuilderFactory;

    .line 19
    .line 20
    .line 21
    move-result-object v9
    :try_end_15
    .catch Ljava/lang/Exception; {:try_start_11 .. :try_end_15} :catch_21

    .line 22
    const/4 v10, 0x1

    .line 23
    :try_start_16
    const-string v11, "http://xml.org/sax/features/external-general-entities"

    .line 24
    .line 25
    invoke-virtual {v9, v11, v8}, Ljavax/xml/parsers/DocumentBuilderFactory;->setFeature(Ljava/lang/String;Z)V

    .line 26
    .line 27
    .line 28
    const-string v11, "http://apache.org/xml/features/disallow-doctype-decl"

    .line 29
    .line 30
    invoke-virtual {v9, v11, v10}, Ljavax/xml/parsers/DocumentBuilderFactory;->setFeature(Ljava/lang/String;Z)V
    :try_end_20
    .catch Ljavax/xml/parsers/ParserConfigurationException; {:try_start_16 .. :try_end_20} :catch_24
    .catch Ljava/lang/Exception; {:try_start_16 .. :try_end_20} :catch_21

    .line 31
    .line 32
    .line 33
    goto :goto_24

    .line 34
    :catch_21
    move-exception v0

    .line 35
    goto/16 :goto_e8

    .line 36
    .line 37
    :catch_24
    :goto_24
    :try_start_24
    invoke-virtual {v9}, Ljavax/xml/parsers/DocumentBuilderFactory;->newDocumentBuilder()Ljavax/xml/parsers/DocumentBuilder;

    .line 38
    .line 39
    .line 40
    move-result-object v9

    .line 41
    move-object/from16 v11, p1

    .line 42
    .line 43
    invoke-virtual {v9, v11}, Ljavax/xml/parsers/DocumentBuilder;->parse(Ljava/io/InputStream;)Lorg/w3c/dom/Document;

    .line 44
    .line 45
    .line 46
    move-result-object v9

    .line 47
    const-string v11, "resource"

    .line 48
    .line 49
    invoke-interface {v9, v11}, Lorg/w3c/dom/Document;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 50
    .line 51
    .line 52
    move-result-object v9

    .line 53
    invoke-interface {v9}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 54
    .line 55
    .line 56
    move-result v11

    .line 57
    if-ge v11, v10, :cond_3c

    .line 58
    .line 59
    goto/16 :goto_e4

    .line 60
    .line 61
    :cond_3c
    invoke-interface {v9, v8}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 62
    .line 63
    .line 64
    move-result-object v9

    .line 65
    check-cast v9, Lorg/w3c/dom/Element;

    .line 66
    .line 67
    invoke-interface {v9, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 68
    .line 69
    .line 70
    move-result v11

    .line 71
    if-eqz v11, :cond_4e

    .line 72
    .line 73
    invoke-interface {v9, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 74
    .line 75
    .line 76
    move-result-object v6

    .line 77
    iput-object v6, v1, Lf/c85;->zZ1:Ljava/lang/String;

    .line 78
    .line 79
    :cond_4e
    invoke-interface {v9, v5}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 80
    .line 81
    .line 82
    move-result v6

    .line 83
    if-eqz v6, :cond_5a

    .line 84
    .line 85
    invoke-interface {v9, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 86
    .line 87
    .line 88
    move-result-object v5

    .line 89
    iput-object v5, v1, Lf/c85;->DD:Ljava/lang/String;

    .line 90
    .line 91
    :cond_5a
    invoke-interface {v9, v4}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 92
    .line 93
    .line 94
    move-result v5

    .line 95
    if-eqz v5, :cond_66

    .line 96
    .line 97
    invoke-interface {v9, v4}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 98
    .line 99
    .line 100
    move-result-object v4

    .line 101
    iput-object v4, v1, Lf/c85;->O2:Ljava/lang/String;

    .line 102
    .line 103
    :cond_66
    invoke-interface {v9, v3}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 104
    .line 105
    .line 106
    move-result v4

    .line 107
    if-eqz v4, :cond_72

    .line 108
    .line 109
    invoke-interface {v9, v3}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 110
    .line 111
    .line 112
    move-result-object v3

    .line 113
    iput-object v3, v1, Lf/c85;->final:Ljava/lang/String;

    .line 114
    .line 115
    :cond_72
    invoke-interface {v9, v0}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 116
    .line 117
    .line 118
    move-result v3

    .line 119
    if-eqz v3, :cond_c9

    .line 120
    .line 121
    invoke-interface {v9, v0}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 122
    .line 123
    .line 124
    move-result-object v11

    .line 125
    iput-object v11, v1, Lf/c85;->Zl0:Ljava/lang/String;

    .line 126
    .line 127
    const-string v14, "https://forums.pokemmo.eu"

    .line 128
    .line 129
    sget-object v0, Lf/ay0;->EV:Lf/xv7;

    .line 130
    .line 131
    const/4 v15, 0x0

    .line 132
    const/16 v16, 0x19

    .line 133
    .line 134
    const/4 v12, 0x1

    .line 135
    const/4 v13, 0x0

    .line 136
    invoke-virtual/range {v11 .. v16}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    .line 137
    .line 138
    .line 139
    move-result v0

    .line 140
    if-nez v0, :cond_c9

    .line 141
    .line 142
    iget-object v11, v1, Lf/c85;->Zl0:Ljava/lang/String;

    .line 143
    .line 144
    const-string v14, "https://pokemmo.eu"

    .line 145
    .line 146
    const/4 v15, 0x0

    .line 147
    const/16 v16, 0x12

    .line 148
    .line 149
    const/4 v12, 0x1

    .line 150
    const/4 v13, 0x0

    .line 151
    invoke-virtual/range {v11 .. v16}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    .line 152
    .line 153
    .line 154
    move-result v0

    .line 155
    if-nez v0, :cond_c9

    .line 156
    .line 157
    iget-object v11, v1, Lf/c85;->Zl0:Ljava/lang/String;

    .line 158
    .line 159
    const-string v14, "https://forums.pokemmo.com"

    .line 160
    .line 161
    const/4 v15, 0x0

    .line 162
    const/16 v16, 0x1a

    .line 163
    .line 164
    const/4 v12, 0x1

    .line 165
    const/4 v13, 0x0

    .line 166
    invoke-virtual/range {v11 .. v16}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    .line 167
    .line 168
    .line 169
    move-result v0

    .line 170
    if-nez v0, :cond_c9

    .line 171
    .line 172
    iget-object v11, v1, Lf/c85;->Zl0:Ljava/lang/String;

    .line 173
    .line 174
    const-string v14, "https://pokemmo.com"

    .line 175
    .line 176
    const/4 v15, 0x0

    .line 177
    const/16 v16, 0x13

    .line 178
    .line 179
    const/4 v12, 0x1

    .line 180
    const/4 v13, 0x0

    .line 181
    invoke-virtual/range {v11 .. v16}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    .line 182
    .line 183
    .line 184
    move-result v0

    .line 185
    if-nez v0, :cond_c9

    .line 186
    .line 187
    const-string v0, "weblink must start with https://forums.pokemmo.com; {}"

    .line 188
    .line 189
    iget-object v3, v7, Lf/z46;->O01:Ljava/io/File;

    .line 190
    .line 191
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 192
    .line 193
    .line 194
    move-result-object v3

    .line 195
    invoke-interface {v2, v0, v3}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 196
    .line 197
    .line 198
    const-string v0, ""

    .line 199
    .line 200
    iput-object v0, v1, Lf/c85;->Zl0:Ljava/lang/String;

    .line 201
    .line 202
    :cond_c9
    invoke-virtual {v1, v9}, Lf/c85;->z20(Lorg/w3c/dom/Element;)Z

    .line 203
    .line 204
    .line 205
    move-result v0

    .line 206
    if-nez v0, :cond_d0

    .line 207
    .line 208
    goto :goto_e4

    .line 209
    :cond_d0
    invoke-virtual {v1, v9}, Lf/c85;->m31(Lorg/w3c/dom/Element;)Z

    .line 210
    .line 211
    .line 212
    move-result v0

    .line 213
    if-nez v0, :cond_d7

    .line 214
    .line 215
    goto :goto_e4

    .line 216
    :cond_d7
    invoke-virtual {v1, v9}, Lf/c85;->D(Lorg/w3c/dom/Element;)Z

    .line 217
    .line 218
    .line 219
    move-result v0

    .line 220
    if-nez v0, :cond_de

    .line 221
    .line 222
    goto :goto_e4

    .line 223
    :cond_de
    invoke-virtual {v1, v9}, Lf/c85;->VA(Lorg/w3c/dom/Element;)Z

    .line 224
    .line 225
    .line 226
    move-result v0

    .line 227
    if-nez v0, :cond_e5

    .line 228
    .line 229
    :goto_e4
    return v8

    .line 230
    :cond_e5
    iput-boolean v10, v1, Lf/c85;->FX0:Z
    :try_end_e7
    .catch Ljava/lang/Exception; {:try_start_24 .. :try_end_e7} :catch_21

    .line 231
    .line 232
    return v10

    .line 233
    :goto_e8
    iget-object v3, v7, Lf/z46;->O01:Ljava/io/File;

    .line 234
    .line 235
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 236
    .line 237
    .line 238
    move-result-object v3

    .line 239
    sget-object v4, Lf/c85;->zL1:Lf/xv7;

    .line 240
    .line 241
    const-string v5, "Error loading mod info.xml {}"

    .line 242
    .line 243
    invoke-interface {v4, v5, v3, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 244
    .line 245
    .line 246
    iget-object v3, v7, Lf/z46;->O01:Ljava/io/File;

    .line 247
    .line 248
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 249
    .line 250
    .line 251
    move-result-object v3

    .line 252
    invoke-interface {v2, v5, v3, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 253
    .line 254
    .line 255
    return v8
.end method

.method public final m31(Lorg/w3c/dom/Element;)Z
    .registers 15

    .line 1
    const-string v0, "theme_extensions"

    .line 2
    .line 3
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 4
    .line 5
    .line 6
    move-result-object p1

    .line 7
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    sget-object v1, Lf/c85;->jL:Lf/xv7;

    .line 12
    .line 13
    const/4 v2, 0x0

    .line 14
    const/4 v3, 0x1

    .line 15
    if-le v0, v3, :cond_16

    .line 16
    .line 17
    const-string p1, "Mods are only allowed to define one \'theme_extensions\' section"

    .line 18
    .line 19
    invoke-interface {v1, p1}, Lf/xv7;->error(Ljava/lang/String;)V

    .line 20
    .line 21
    .line 22
    return v2

    .line 23
    :cond_16
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 24
    .line 25
    .line 26
    move-result v0

    .line 27
    if-ne v0, v3, :cond_140

    .line 28
    .line 29
    invoke-interface {p1, v2}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 30
    .line 31
    .line 32
    move-result-object p1

    .line 33
    check-cast p1, Lorg/w3c/dom/Element;

    .line 34
    .line 35
    const-string v0, "theme_extension"

    .line 36
    .line 37
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 38
    .line 39
    .line 40
    move-result-object v0

    .line 41
    const/4 v4, 0x0

    .line 42
    :goto_29
    invoke-interface {v0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 43
    .line 44
    .line 45
    move-result v5

    .line 46
    if-ge v4, v5, :cond_140

    .line 47
    .line 48
    invoke-interface {v0, v4}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 49
    .line 50
    .line 51
    move-result-object v5

    .line 52
    check-cast v5, Lorg/w3c/dom/Element;

    .line 53
    .line 54
    const-string v6, "theme_extension_revision"

    .line 55
    .line 56
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 57
    .line 58
    .line 59
    move-result v7

    .line 60
    const-string v8, "revision"

    .line 61
    .line 62
    if-eqz v7, :cond_46

    .line 63
    .line 64
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 65
    .line 66
    .line 67
    move-result-object v6

    .line 68
    invoke-interface {v5, v8, v6}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 69
    .line 70
    .line 71
    :cond_46
    const-string v6, "path"

    .line 72
    .line 73
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 74
    .line 75
    .line 76
    move-result v6

    .line 77
    iget-object v7, p0, Lf/c85;->BP1:Lf/z46;

    .line 78
    .line 79
    if-nez v6, :cond_5c

    .line 80
    .line 81
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 82
    .line 83
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 84
    .line 85
    .line 86
    move-result-object p1

    .line 87
    const-string v0, "Theme-Extension has no path attribute: {}"

    .line 88
    .line 89
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 90
    .line 91
    .line 92
    return v2

    .line 93
    :cond_5c
    const-string v6, "name"

    .line 94
    .line 95
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 96
    .line 97
    .line 98
    move-result v9

    .line 99
    if-nez v9, :cond_70

    .line 100
    .line 101
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 102
    .line 103
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 104
    .line 105
    .line 106
    move-result-object p1

    .line 107
    const-string v0, "Theme-Extension has no name attribute: {}"

    .line 108
    .line 109
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 110
    .line 111
    .line 112
    return v2

    .line 113
    :cond_70
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 114
    .line 115
    .line 116
    move-result-object v6

    .line 117
    const-string v9, "android"

    .line 118
    .line 119
    invoke-virtual {v6, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 120
    .line 121
    .line 122
    move-result v9

    .line 123
    if-nez v9, :cond_134

    .line 124
    .line 125
    const-string v9, "default"

    .line 126
    .line 127
    invoke-virtual {v6, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 128
    .line 129
    .line 130
    move-result v9

    .line 131
    if-eqz v9, :cond_86

    .line 132
    .line 133
    goto/16 :goto_134

    .line 134
    .line 135
    :cond_86
    iget-object v9, p0, Lf/c85;->uP0:Ljava/util/ArrayList;

    .line 136
    .line 137
    invoke-static {v9}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 138
    .line 139
    .line 140
    move-result-object v10

    .line 141
    new-instance v11, Lf/oc7;

    .line 142
    .line 143
    const/16 v12, 0x16

    .line 144
    .line 145
    invoke-direct {v11, v12}, Lf/oc7;-><init>(I)V

    .line 146
    .line 147
    .line 148
    invoke-interface {v10, v11}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 149
    .line 150
    .line 151
    move-result-object v10

    .line 152
    new-instance v11, Lf/vn4;

    .line 153
    .line 154
    const/16 v12, 0xc

    .line 155
    .line 156
    invoke-direct {v11, v6, v12}, Lf/vn4;-><init>(Ljava/lang/String;I)V

    .line 157
    .line 158
    .line 159
    invoke-interface {v10, v11}, Lj$/util/stream/Stream;->anyMatch(Ljava/util/function/Predicate;)Z

    .line 160
    .line 161
    .line 162
    move-result v10

    .line 163
    if-eqz v10, :cond_b0

    .line 164
    .line 165
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 166
    .line 167
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 168
    .line 169
    .line 170
    move-result-object p1

    .line 171
    const-string v0, "Theme-Extension with duplicate name {}: {}"

    .line 172
    .line 173
    invoke-interface {v1, v0, v6, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 174
    .line 175
    .line 176
    return v2

    .line 177
    :cond_b0
    const-string v10, "is_mobile"

    .line 178
    .line 179
    invoke-interface {v5, v10}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 180
    .line 181
    .line 182
    move-result v11

    .line 183
    if-nez v11, :cond_c4

    .line 184
    .line 185
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 186
    .line 187
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 188
    .line 189
    .line 190
    move-result-object p1

    .line 191
    const-string v0, "Theme-Extension has no is_mobile attribute: {}"

    .line 192
    .line 193
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 194
    .line 195
    .line 196
    return v2

    .line 197
    :cond_c4
    invoke-interface {v5, v10}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 198
    .line 199
    .line 200
    move-result-object v10

    .line 201
    const-string v11, "true"

    .line 202
    .line 203
    invoke-virtual {v10, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 204
    .line 205
    .line 206
    move-result v11

    .line 207
    if-nez v11, :cond_e4

    .line 208
    .line 209
    const-string v11, "false"

    .line 210
    .line 211
    invoke-virtual {v10, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 212
    .line 213
    .line 214
    move-result v11

    .line 215
    if-nez v11, :cond_e4

    .line 216
    .line 217
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 218
    .line 219
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 220
    .line 221
    .line 222
    move-result-object p1

    .line 223
    const-string v0, "is_mobile {} is neither \'true\' nor \'false\': {}"

    .line 224
    .line 225
    invoke-interface {v1, v0, v10, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 226
    .line 227
    .line 228
    return v2

    .line 229
    :cond_e4
    invoke-virtual {p0, v5, v6}, Lf/c85;->cE1(Lorg/w3c/dom/Element;Ljava/lang/String;)Z

    .line 230
    .line 231
    .line 232
    move-result v6

    .line 233
    if-nez v6, :cond_eb

    .line 234
    .line 235
    return v2

    .line 236
    :cond_eb
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 237
    .line 238
    .line 239
    move-result v6

    .line 240
    if-nez v6, :cond_fd

    .line 241
    .line 242
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 243
    .line 244
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 245
    .line 246
    .line 247
    move-result-object p1

    .line 248
    const-string v0, "Theme-Extension has no revision attribute: {}"

    .line 249
    .line 250
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 251
    .line 252
    .line 253
    return v2

    .line 254
    :cond_fd
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 255
    .line 256
    .line 257
    move-result-object v6

    .line 258
    :try_start_101
    invoke-static {v6}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 259
    .line 260
    .line 261
    move-result v8

    .line 262
    if-le v8, v3, :cond_121

    .line 263
    .line 264
    const-string p1, "Theme-Extension revision {} is above current revision {}: {}"

    .line 265
    .line 266
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 267
    .line 268
    .line 269
    move-result-object v0

    .line 270
    iget-object v4, v7, Lf/z46;->O01:Ljava/io/File;

    .line 271
    .line 272
    invoke-virtual {v4}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 273
    .line 274
    .line 275
    move-result-object v4

    .line 276
    const/4 v5, 0x3

    .line 277
    new-array v5, v5, [Ljava/lang/Object;

    .line 278
    .line 279
    aput-object v6, v5, v2

    .line 280
    .line 281
    aput-object v0, v5, v3

    .line 282
    .line 283
    const/4 v0, 0x2

    .line 284
    aput-object v4, v5, v0

    .line 285
    .line 286
    invoke-interface {v1, p1, v5}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V
    :try_end_120
    .catch Ljava/lang/NumberFormatException; {:try_start_101 .. :try_end_120} :catch_128

    .line 287
    .line 288
    .line 289
    return v2

    .line 290
    :cond_121
    invoke-virtual {v9, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 291
    .line 292
    .line 293
    add-int/lit8 v4, v4, 0x1

    .line 294
    .line 295
    goto/16 :goto_29

    .line 296
    .line 297
    :catch_128
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 298
    .line 299
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 300
    .line 301
    .line 302
    move-result-object p1

    .line 303
    const-string v0, "Theme-Extension revision {} is not a number: {}"

    .line 304
    .line 305
    invoke-interface {v1, v0, v6, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 306
    .line 307
    .line 308
    return v2

    .line 309
    :cond_134
    :goto_134
    iget-object p1, v7, Lf/z46;->O01:Ljava/io/File;

    .line 310
    .line 311
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 312
    .line 313
    .line 314
    move-result-object p1

    .line 315
    const-string v0, "Theme-Extension of name `default` or `android` are not allowed: {}"

    .line 316
    .line 317
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 318
    .line 319
    .line 320
    return v2

    .line 321
    :cond_140
    return v3
.end method

.method public final vg()Z
    .registers 25

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    iget-object v1, v0, Lf/c85;->WY1:Ljava/util/ArrayList;

    .line 4
    .line 5
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 6
    .line 7
    .line 8
    move-result v2

    .line 9
    const/4 v3, 0x0

    .line 10
    const/4 v4, 0x0

    .line 11
    :goto_a
    if-ge v4, v2, :cond_157

    .line 12
    .line 13
    invoke-virtual {v1, v4}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 14
    .line 15
    .line 16
    move-result-object v5

    .line 17
    add-int/lit8 v4, v4, 0x1

    .line 18
    .line 19
    check-cast v5, Lorg/w3c/dom/Element;

    .line 20
    .line 21
    const-string v6, "path"

    .line 22
    .line 23
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 24
    .line 25
    .line 26
    move-result-object v6

    .line 27
    iget-object v7, v0, Lf/c85;->DQ:Lf/z46;

    .line 28
    .line 29
    invoke-virtual {v7, v6}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 30
    .line 31
    .line 32
    move-result-object v7

    .line 33
    invoke-virtual {v7}, Lf/z46;->hz1()Z

    .line 34
    .line 35
    .line 36
    move-result v7

    .line 37
    iget-object v8, v0, Lf/c85;->BP1:Lf/z46;

    .line 38
    .line 39
    sget-object v9, Lf/c85;->jL:Lf/xv7;

    .line 40
    .line 41
    if-nez v7, :cond_36

    .line 42
    .line 43
    iget-object v1, v8, Lf/z46;->O01:Ljava/io/File;

    .line 44
    .line 45
    invoke-virtual {v1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 46
    .line 47
    .line 48
    move-result-object v1

    .line 49
    const-string v2, "Path {} does not exist in mod {}"

    .line 50
    .line 51
    invoke-interface {v9, v2, v6, v1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 52
    .line 53
    .line 54
    return v3

    .line 55
    :cond_36
    const-string v7, "name"

    .line 56
    .line 57
    invoke-interface {v5, v7}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 58
    .line 59
    .line 60
    move-result-object v11

    .line 61
    const-string v10, "is_mobile"

    .line 62
    .line 63
    invoke-interface {v5, v10}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 64
    .line 65
    .line 66
    move-result-object v10

    .line 67
    invoke-static {v10}, Ljava/lang/Boolean;->parseBoolean(Ljava/lang/String;)Z

    .line 68
    .line 69
    .line 70
    move-result v13

    .line 71
    const-string v10, "sprite_atlas"

    .line 72
    .line 73
    invoke-interface {v5, v10}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 74
    .line 75
    .line 76
    move-result v12

    .line 77
    if-eqz v12, :cond_6c

    .line 78
    .line 79
    invoke-interface {v5, v10}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 80
    .line 81
    .line 82
    move-result-object v10

    .line 83
    iget-object v12, v0, Lf/c85;->DQ:Lf/z46;

    .line 84
    .line 85
    invoke-virtual {v12, v10}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 86
    .line 87
    .line 88
    move-result-object v12

    .line 89
    invoke-virtual {v12}, Lf/z46;->hz1()Z

    .line 90
    .line 91
    .line 92
    move-result v14

    .line 93
    if-nez v14, :cond_6a

    .line 94
    .line 95
    iget-object v1, v8, Lf/z46;->O01:Ljava/io/File;

    .line 96
    .line 97
    invoke-virtual {v1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 98
    .line 99
    .line 100
    move-result-object v1

    .line 101
    const-string v2, "Atlas path {} does not exist in mod {}"

    .line 102
    .line 103
    invoke-interface {v9, v2, v10, v1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 104
    .line 105
    .line 106
    return v3

    .line 107
    :cond_6a
    :goto_6a
    move-object v14, v12

    .line 108
    goto :goto_6e

    .line 109
    :cond_6c
    const/4 v12, 0x0

    .line 110
    goto :goto_6a

    .line 111
    :goto_6e
    new-instance v10, Ljava/lang/StringBuilder;

    .line 112
    .line 113
    const-string v12, "theme-"

    .line 114
    .line 115
    invoke-direct {v10, v12}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 116
    .line 117
    .line 118
    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 119
    .line 120
    .line 121
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 122
    .line 123
    .line 124
    move-result-object v10

    .line 125
    invoke-static {v5, v10}, Lf/c85;->TH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Ljava/util/ArrayList;

    .line 126
    .line 127
    .line 128
    move-result-object v15

    .line 129
    new-instance v10, Ljava/util/ArrayList;

    .line 130
    .line 131
    invoke-direct {v10}, Ljava/util/ArrayList;-><init>()V

    .line 132
    .line 133
    .line 134
    invoke-static {v15}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 135
    .line 136
    .line 137
    move-result-object v12

    .line 138
    new-instance v3, Lf/oc7;

    .line 139
    .line 140
    move-object/from16 v19, v1

    .line 141
    .line 142
    const/16 v1, 0x17

    .line 143
    .line 144
    invoke-direct {v3, v1}, Lf/oc7;-><init>(I)V

    .line 145
    .line 146
    .line 147
    invoke-static {}, Lj$/util/function/Function$-CC;->identity()Ljava/util/function/Function;

    .line 148
    .line 149
    .line 150
    move-result-object v1

    .line 151
    invoke-static {v3, v1}, Lj$/util/stream/Collectors;->toMap(Ljava/util/function/Function;Ljava/util/function/Function;)Lj$/util/stream/Collector;

    .line 152
    .line 153
    .line 154
    move-result-object v1

    .line 155
    invoke-interface {v12, v1}, Lj$/util/stream/Stream;->collect(Lj$/util/stream/Collector;)Ljava/lang/Object;

    .line 156
    .line 157
    .line 158
    move-result-object v1

    .line 159
    check-cast v1, Ljava/util/Map;

    .line 160
    .line 161
    const-string v3, "constants_presets"

    .line 162
    .line 163
    invoke-static {v5, v3}, Lf/c85;->QH0(Lorg/w3c/dom/Element;Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 164
    .line 165
    .line 166
    move-result-object v3

    .line 167
    const-string v12, "constants_preset"

    .line 168
    .line 169
    invoke-interface {v3, v12}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 170
    .line 171
    .line 172
    move-result-object v3

    .line 173
    move/from16 v20, v2

    .line 174
    .line 175
    const/4 v12, 0x0

    .line 176
    :goto_af
    invoke-interface {v3}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 177
    .line 178
    .line 179
    move-result v2

    .line 180
    if-ge v12, v2, :cond_108

    .line 181
    .line 182
    invoke-interface {v3, v12}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 183
    .line 184
    .line 185
    move-result-object v2

    .line 186
    check-cast v2, Lorg/w3c/dom/Element;

    .line 187
    .line 188
    move-object/from16 v16, v3

    .line 189
    .line 190
    invoke-interface {v2, v7}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 191
    .line 192
    .line 193
    move-result-object v3

    .line 194
    move/from16 v21, v4

    .line 195
    .line 196
    new-instance v4, Ljava/util/HashMap;

    .line 197
    .line 198
    invoke-direct {v4}, Ljava/util/HashMap;-><init>()V

    .line 199
    .line 200
    .line 201
    move-object/from16 v17, v11

    .line 202
    .line 203
    const-string v11, "constant"

    .line 204
    .line 205
    invoke-interface {v2, v11}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 206
    .line 207
    .line 208
    move-result-object v2

    .line 209
    move/from16 v22, v12

    .line 210
    .line 211
    const/4 v11, 0x0

    .line 212
    :goto_d3
    invoke-interface {v2}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 213
    .line 214
    .line 215
    move-result v12

    .line 216
    if-ge v11, v12, :cond_f7

    .line 217
    .line 218
    invoke-interface {v2, v11}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 219
    .line 220
    .line 221
    move-result-object v12

    .line 222
    check-cast v12, Lorg/w3c/dom/Element;

    .line 223
    .line 224
    move-object/from16 v23, v2

    .line 225
    .line 226
    invoke-interface {v12, v7}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 227
    .line 228
    .line 229
    move-result-object v2

    .line 230
    invoke-interface {v12}, Lorg/w3c/dom/Node;->getTextContent()Ljava/lang/String;

    .line 231
    .line 232
    .line 233
    move-result-object v12

    .line 234
    invoke-interface {v1, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 235
    .line 236
    .line 237
    move-result-object v2

    .line 238
    check-cast v2, Lf/wh4;

    .line 239
    .line 240
    invoke-virtual {v4, v2, v12}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 241
    .line 242
    .line 243
    add-int/lit8 v11, v11, 0x1

    .line 244
    .line 245
    move-object/from16 v2, v23

    .line 246
    .line 247
    goto :goto_d3

    .line 248
    :cond_f7
    new-instance v2, Lf/ci4;

    .line 249
    .line 250
    invoke-direct {v2, v3, v4}, Lf/ci4;-><init>(Ljava/lang/String;Ljava/util/HashMap;)V

    .line 251
    .line 252
    .line 253
    invoke-virtual {v10, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 254
    .line 255
    .line 256
    add-int/lit8 v12, v22, 0x1

    .line 257
    .line 258
    move-object/from16 v3, v16

    .line 259
    .line 260
    move-object/from16 v11, v17

    .line 261
    .line 262
    move/from16 v4, v21

    .line 263
    .line 264
    goto :goto_af

    .line 265
    :cond_108
    move/from16 v21, v4

    .line 266
    .line 267
    move-object/from16 v17, v11

    .line 268
    .line 269
    new-instance v1, Lf/px0;

    .line 270
    .line 271
    invoke-direct {v1}, Lf/px0;-><init>()V

    .line 272
    .line 273
    .line 274
    invoke-virtual {v1}, Lf/px0;->BE()V

    .line 275
    .line 276
    .line 277
    iget-object v2, v0, Lf/c85;->DQ:Lf/z46;

    .line 278
    .line 279
    iget-object v3, v1, Lf/px0;->m9:Ljava/util/List;

    .line 280
    .line 281
    const/4 v4, 0x0

    .line 282
    invoke-interface {v3, v4, v2}, Ljava/util/List;->add(ILjava/lang/Object;)V

    .line 283
    .line 284
    .line 285
    move-object/from16 v16, v10

    .line 286
    .line 287
    new-instance v10, Lf/xw5;

    .line 288
    .line 289
    invoke-virtual {v1, v6}, Lf/px0;->yZ0(Ljava/lang/String;)Lf/v9;

    .line 290
    .line 291
    .line 292
    move-result-object v12

    .line 293
    const-string v1, "revision"

    .line 294
    .line 295
    invoke-interface {v5, v1}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 296
    .line 297
    .line 298
    move-result-object v1

    .line 299
    invoke-static {v1}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 300
    .line 301
    .line 302
    move-result v1

    .line 303
    move/from16 v17, v1

    .line 304
    .line 305
    invoke-direct/range {v10 .. v17}, Lf/xw5;-><init>(Ljava/lang/String;Lf/v9;ZLf/z46;Ljava/util/List;Ljava/util/List;I)V

    .line 306
    .line 307
    .line 308
    invoke-virtual {v10}, Lf/xw5;->nB1()Z

    .line 309
    .line 310
    .line 311
    move-result v1

    .line 312
    if-nez v1, :cond_147

    .line 313
    .line 314
    iget-object v1, v8, Lf/z46;->O01:Ljava/io/File;

    .line 315
    .line 316
    invoke-virtual {v1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 317
    .line 318
    .line 319
    move-result-object v1

    .line 320
    const-string v2, "Theme of name {} in mod {} is not valid"

    .line 321
    .line 322
    invoke-interface {v9, v2, v11, v1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 323
    .line 324
    .line 325
    const/16 v18, 0x0

    .line 326
    .line 327
    return v18

    .line 328
    :cond_147
    const/16 v18, 0x0

    .line 329
    .line 330
    iget-object v1, v0, Lf/c85;->ek:Ljava/util/ArrayList;

    .line 331
    .line 332
    invoke-virtual {v1, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 333
    .line 334
    .line 335
    move-object/from16 v1, v19

    .line 336
    .line 337
    move/from16 v2, v20

    .line 338
    .line 339
    move/from16 v4, v21

    .line 340
    .line 341
    const/4 v3, 0x0

    .line 342
    goto/16 :goto_a

    .line 343
    .line 344
    :cond_157
    const/4 v1, 0x1

    .line 345
    return v1
.end method

.method public final xb0()Z
    .registers 11

    .line 1
    iget-object v0, p0, Lf/c85;->hQ0:Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    const/4 v2, 0x0

    .line 8
    const/4 v3, 0x0

    .line 9
    :goto_8
    if-ge v3, v1, :cond_58

    .line 10
    .line 11
    invoke-virtual {v0, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 12
    .line 13
    .line 14
    move-result-object v4

    .line 15
    add-int/lit8 v3, v3, 0x1

    .line 16
    .line 17
    check-cast v4, Lorg/w3c/dom/Element;

    .line 18
    .line 19
    const-string v5, "path"

    .line 20
    .line 21
    invoke-interface {v4, v5}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 22
    .line 23
    .line 24
    move-result-object v5

    .line 25
    iget-object v6, p0, Lf/c85;->DQ:Lf/z46;

    .line 26
    .line 27
    invoke-virtual {v6, v5}, Lf/z46;->pI1(Ljava/lang/String;)Lf/z46;

    .line 28
    .line 29
    .line 30
    move-result-object v6

    .line 31
    invoke-virtual {v6}, Lf/z46;->hz1()Z

    .line 32
    .line 33
    .line 34
    move-result v7

    .line 35
    iget-object v8, p0, Lf/c85;->BP1:Lf/z46;

    .line 36
    .line 37
    sget-object v9, Lf/c85;->jL:Lf/xv7;

    .line 38
    .line 39
    if-nez v7, :cond_34

    .line 40
    .line 41
    iget-object v0, v8, Lf/z46;->O01:Ljava/io/File;

    .line 42
    .line 43
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 44
    .line 45
    .line 46
    move-result-object v0

    .line 47
    const-string v1, "Path {} does not exist in mod {}"

    .line 48
    .line 49
    :goto_30
    invoke-interface {v9, v1, v5, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 50
    .line 51
    .line 52
    return v2

    .line 53
    :cond_34
    const-string v7, "revision"

    .line 54
    .line 55
    invoke-interface {v4, v7}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 56
    .line 57
    .line 58
    move-result-object v4

    .line 59
    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 60
    .line 61
    .line 62
    move-result v4

    .line 63
    new-instance v7, Lf/g40;

    .line 64
    .line 65
    invoke-direct {v7, v6, v4, v2}, Lf/g40;-><init>(Lf/z46;IZ)V

    .line 66
    .line 67
    .line 68
    invoke-virtual {v7}, Lf/g40;->yC1()Z

    .line 69
    .line 70
    .line 71
    move-result v4

    .line 72
    if-nez v4, :cond_52

    .line 73
    .line 74
    iget-object v0, v8, Lf/z46;->O01:Ljava/io/File;

    .line 75
    .line 76
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 77
    .line 78
    .line 79
    move-result-object v0

    .line 80
    const-string v1, "String with path {} in mod {} is not valid"

    .line 81
    .line 82
    goto :goto_30

    .line 83
    :cond_52
    iget-object v4, p0, Lf/c85;->mx1:Ljava/util/ArrayList;

    .line 84
    .line 85
    invoke-virtual {v4, v7}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 86
    .line 87
    .line 88
    goto :goto_8

    .line 89
    :cond_58
    const/4 v0, 0x1

    .line 90
    return v0
.end method

.method public final z20(Lorg/w3c/dom/Element;)Z
    .registers 16

    .line 1
    const-string v0, "themes"

    .line 2
    .line 3
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 4
    .line 5
    .line 6
    move-result-object p1

    .line 7
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    sget-object v1, Lf/c85;->jL:Lf/xv7;

    .line 12
    .line 13
    const/4 v2, 0x0

    .line 14
    const/4 v3, 0x1

    .line 15
    if-le v0, v3, :cond_16

    .line 16
    .line 17
    const-string p1, "Mods are only allowed to define on \'themes\' section"

    .line 18
    .line 19
    invoke-interface {v1, p1}, Lf/xv7;->error(Ljava/lang/String;)V

    .line 20
    .line 21
    .line 22
    return v2

    .line 23
    :cond_16
    invoke-interface {p1}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 24
    .line 25
    .line 26
    move-result v0

    .line 27
    if-ne v0, v3, :cond_15d

    .line 28
    .line 29
    invoke-interface {p1, v2}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 30
    .line 31
    .line 32
    move-result-object p1

    .line 33
    check-cast p1, Lorg/w3c/dom/Element;

    .line 34
    .line 35
    const-string v0, "theme"

    .line 36
    .line 37
    invoke-interface {p1, v0}, Lorg/w3c/dom/Element;->getElementsByTagName(Ljava/lang/String;)Lorg/w3c/dom/NodeList;

    .line 38
    .line 39
    .line 40
    move-result-object v0

    .line 41
    const/4 v4, 0x0

    .line 42
    :goto_29
    invoke-interface {v0}, Lorg/w3c/dom/NodeList;->getLength()I

    .line 43
    .line 44
    .line 45
    move-result v5

    .line 46
    if-ge v4, v5, :cond_15d

    .line 47
    .line 48
    invoke-interface {v0, v4}, Lorg/w3c/dom/NodeList;->item(I)Lorg/w3c/dom/Node;

    .line 49
    .line 50
    .line 51
    move-result-object v5

    .line 52
    check-cast v5, Lorg/w3c/dom/Element;

    .line 53
    .line 54
    const-string v6, "theme_revision"

    .line 55
    .line 56
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 57
    .line 58
    .line 59
    move-result v7

    .line 60
    const-string v8, "revision"

    .line 61
    .line 62
    if-eqz v7, :cond_46

    .line 63
    .line 64
    invoke-interface {p1, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 65
    .line 66
    .line 67
    move-result-object v6

    .line 68
    invoke-interface {v5, v8, v6}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 69
    .line 70
    .line 71
    :cond_46
    const-string v6, "path"

    .line 72
    .line 73
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 74
    .line 75
    .line 76
    move-result v7

    .line 77
    iget-object v9, p0, Lf/c85;->BP1:Lf/z46;

    .line 78
    .line 79
    if-nez v7, :cond_5c

    .line 80
    .line 81
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 82
    .line 83
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 84
    .line 85
    .line 86
    move-result-object p1

    .line 87
    const-string v0, "Theme has no path attribute: {}"

    .line 88
    .line 89
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 90
    .line 91
    .line 92
    return v2

    .line 93
    :cond_5c
    const-string v7, "name"

    .line 94
    .line 95
    invoke-interface {v5, v7}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 96
    .line 97
    .line 98
    move-result v10

    .line 99
    if-nez v10, :cond_70

    .line 100
    .line 101
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 102
    .line 103
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 104
    .line 105
    .line 106
    move-result-object p1

    .line 107
    const-string v0, "Theme has no name attribute: {}"

    .line 108
    .line 109
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 110
    .line 111
    .line 112
    return v2

    .line 113
    :cond_70
    invoke-interface {v5, v7}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 114
    .line 115
    .line 116
    move-result-object v7

    .line 117
    const-string v10, "android"

    .line 118
    .line 119
    invoke-virtual {v7, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 120
    .line 121
    .line 122
    move-result v10

    .line 123
    if-nez v10, :cond_151

    .line 124
    .line 125
    const-string v10, "default"

    .line 126
    .line 127
    invoke-virtual {v7, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 128
    .line 129
    .line 130
    move-result v10

    .line 131
    if-eqz v10, :cond_86

    .line 132
    .line 133
    goto/16 :goto_151

    .line 134
    .line 135
    :cond_86
    iget-object v10, p0, Lf/c85;->WY1:Ljava/util/ArrayList;

    .line 136
    .line 137
    invoke-static {v10}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 138
    .line 139
    .line 140
    move-result-object v11

    .line 141
    new-instance v12, Lf/oc7;

    .line 142
    .line 143
    const/16 v13, 0x15

    .line 144
    .line 145
    invoke-direct {v12, v13}, Lf/oc7;-><init>(I)V

    .line 146
    .line 147
    .line 148
    invoke-interface {v11, v12}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 149
    .line 150
    .line 151
    move-result-object v11

    .line 152
    new-instance v12, Lf/vn4;

    .line 153
    .line 154
    const/16 v13, 0xd

    .line 155
    .line 156
    invoke-direct {v12, v7, v13}, Lf/vn4;-><init>(Ljava/lang/String;I)V

    .line 157
    .line 158
    .line 159
    invoke-interface {v11, v12}, Lj$/util/stream/Stream;->anyMatch(Ljava/util/function/Predicate;)Z

    .line 160
    .line 161
    .line 162
    move-result v11

    .line 163
    if-eqz v11, :cond_b0

    .line 164
    .line 165
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 166
    .line 167
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 168
    .line 169
    .line 170
    move-result-object p1

    .line 171
    const-string v0, "Themes with duplicate name {}: {}"

    .line 172
    .line 173
    invoke-interface {v1, v0, v7, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 174
    .line 175
    .line 176
    return v2

    .line 177
    :cond_b0
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 178
    .line 179
    .line 180
    move-result-object v11

    .line 181
    const-string v12, "/"

    .line 182
    .line 183
    invoke-virtual {v11, v12}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    .line 184
    .line 185
    .line 186
    move-result v13

    .line 187
    if-nez v13, :cond_c3

    .line 188
    .line 189
    invoke-virtual {v11, v12}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 190
    .line 191
    .line 192
    move-result-object v11

    .line 193
    invoke-interface {v5, v6, v11}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 194
    .line 195
    .line 196
    :cond_c3
    const-string v6, "is_mobile"

    .line 197
    .line 198
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 199
    .line 200
    .line 201
    move-result v11

    .line 202
    if-nez v11, :cond_d7

    .line 203
    .line 204
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 205
    .line 206
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 207
    .line 208
    .line 209
    move-result-object p1

    .line 210
    const-string v0, "Theme has no is_mobile attribute: {}"

    .line 211
    .line 212
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 213
    .line 214
    .line 215
    return v2

    .line 216
    :cond_d7
    invoke-interface {v5, v6}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 217
    .line 218
    .line 219
    move-result-object v6

    .line 220
    const-string v11, "true"

    .line 221
    .line 222
    invoke-virtual {v6, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 223
    .line 224
    .line 225
    move-result v11

    .line 226
    if-nez v11, :cond_f7

    .line 227
    .line 228
    const-string v11, "false"

    .line 229
    .line 230
    invoke-virtual {v6, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 231
    .line 232
    .line 233
    move-result v11

    .line 234
    if-nez v11, :cond_f7

    .line 235
    .line 236
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 237
    .line 238
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 239
    .line 240
    .line 241
    move-result-object p1

    .line 242
    const-string v0, "is_mobile {} is neither \'true\' nor \'false\': {}"

    .line 243
    .line 244
    invoke-interface {v1, v0, v6, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 245
    .line 246
    .line 247
    return v2

    .line 248
    :cond_f7
    invoke-virtual {p0, v5, v7}, Lf/c85;->cE1(Lorg/w3c/dom/Element;Ljava/lang/String;)Z

    .line 249
    .line 250
    .line 251
    move-result v6

    .line 252
    if-nez v6, :cond_fe

    .line 253
    .line 254
    return v2

    .line 255
    :cond_fe
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->hasAttribute(Ljava/lang/String;)Z

    .line 256
    .line 257
    .line 258
    move-result v6

    .line 259
    if-nez v6, :cond_110

    .line 260
    .line 261
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 262
    .line 263
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 264
    .line 265
    .line 266
    move-result-object p1

    .line 267
    const-string v0, "Theme has no revision attribute: {}"

    .line 268
    .line 269
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 270
    .line 271
    .line 272
    return v2

    .line 273
    :cond_110
    :try_start_110
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 274
    .line 275
    .line 276
    move-result-object v6

    .line 277
    invoke-static {v6}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 278
    .line 279
    .line 280
    move-result v6

    .line 281
    const/16 v7, 0x8

    .line 282
    .line 283
    if-le v6, v7, :cond_13a

    .line 284
    .line 285
    const-string p1, "Theme revision {} is above current revision {}: {}"

    .line 286
    .line 287
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 288
    .line 289
    .line 290
    move-result-object v0

    .line 291
    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 292
    .line 293
    .line 294
    move-result-object v4

    .line 295
    iget-object v6, v9, Lf/z46;->O01:Ljava/io/File;

    .line 296
    .line 297
    invoke-virtual {v6}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 298
    .line 299
    .line 300
    move-result-object v6

    .line 301
    const/4 v7, 0x3

    .line 302
    new-array v7, v7, [Ljava/lang/Object;

    .line 303
    .line 304
    aput-object v0, v7, v2

    .line 305
    .line 306
    aput-object v4, v7, v3

    .line 307
    .line 308
    const/4 v0, 0x2

    .line 309
    aput-object v6, v7, v0

    .line 310
    .line 311
    invoke-interface {v1, p1, v7}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V
    :try_end_139
    .catch Ljava/lang/NumberFormatException; {:try_start_110 .. :try_end_139} :catch_141

    .line 312
    .line 313
    .line 314
    return v2

    .line 315
    :cond_13a
    invoke-virtual {v10, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 316
    .line 317
    .line 318
    add-int/lit8 v4, v4, 0x1

    .line 319
    .line 320
    goto/16 :goto_29

    .line 321
    .line 322
    :catch_141
    invoke-interface {v5, v8}, Lorg/w3c/dom/Element;->getAttribute(Ljava/lang/String;)Ljava/lang/String;

    .line 323
    .line 324
    .line 325
    move-result-object p1

    .line 326
    iget-object v0, v9, Lf/z46;->O01:Ljava/io/File;

    .line 327
    .line 328
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 329
    .line 330
    .line 331
    move-result-object v0

    .line 332
    const-string v3, "Theme revision {} is not a number: {}"

    .line 333
    .line 334
    invoke-interface {v1, v3, p1, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 335
    .line 336
    .line 337
    return v2

    .line 338
    :cond_151
    :goto_151
    iget-object p1, v9, Lf/z46;->O01:Ljava/io/File;

    .line 339
    .line 340
    invoke-virtual {p1}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 341
    .line 342
    .line 343
    move-result-object p1

    .line 344
    const-string v0, "Themes of name `default` or `android` are not allowed: {}"

    .line 345
    .line 346
    invoke-interface {v1, v0, p1}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 347
    .line 348
    .line 349
    return v2

    .line 350
    :cond_15d
    return v3
.end method
