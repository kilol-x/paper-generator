\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{softeng}[2026/07/14 Software Engineering Document Class]

% 继承 article 类
\LoadClass[12pt,a4paper]{article}

% 引入必要宏包
\RequirePackage{ctex}
\RequirePackage{geometry}
\RequirePackage{graphicx}
\RequirePackage{float}
\RequirePackage{hyperref}
\RequirePackage{enumitem}
\RequirePackage{listings}
\RequirePackage{xcolor}
\RequirePackage{longtable}
\RequirePackage{booktabs}
\RequirePackage{caption}
\RequirePackage{amsmath}
\RequirePackage{array}
\RequirePackage{multirow}
\RequirePackage{setspace}
\RequirePackage{fancyhdr}
\RequirePackage{tabularx}

% 页面设置
\geometry{left=2.5cm,right=2.5cm,top=2.5cm,bottom=2.5cm}

% 超链接设置
\hypersetup{
    colorlinks=true,
    linkcolor=black,
    urlcolor=blue,
    citecolor=black
}

% 页眉页脚
\pagestyle{fancy}
\fancyhf{}
\fancyhead[C]{\small 在线论文文档自动生成系统需求规格说明书}
\fancyfoot[C]{\thepage}
\renewcommand{\headrulewidth}{0.4pt}
\renewcommand{\footrulewidth}{0.4pt}

% 行距
\setstretch{1.25}

% ========== 自定义命令 ==========

% 封面命令
\newcommand{\doccover}[3]{
    \begin{titlepage}
        \begin{center}
            \vspace*{2cm}
            \zihao{0}\textbf{在线论文文档自动生成系统} \\
            \vspace{0.5cm}
            \zihao{2}\textbf{#1} \\
            \vspace{1cm}
            \zihao{3}\textbf{版本：#3} \\
            \vspace{4cm}
            \zihao{4}
            \begin{tabular}{rl}
                项目名称： & #2 \\
                文档类型： & #1 \\
                版本号：   & #3 \\
                编写日期： & \today \\
            \end{tabular} \\
            \vspace{3cm}
            \zihao{3} 实训项目组 \\
            \vspace{0.5cm}
            \zihao{4} \today
        \end{center}
    \end{titlepage}
}

% 修订记录环境
\newenvironment{revrecord}{
    \section*{修订记录}
    \begin{longtable}{|p{2cm}|p{3cm}|p{2.5cm}|p{2.5cm}|p{3.5cm}|}
        \hline
        \textbf{版本号} & \textbf{修订日期} & \textbf{修订人} & \textbf{审核人} & \textbf{修订说明} \\
        \hline
        \endhead
}{
    \end{longtable}
}

% 需求条目命令
\newcommand{\reqitem}[2]{
    \noindent \textbf{#1}：{#2} \\
}

% ========== 代码块样式 ==========
\lstset{
    basicstyle=\small\ttfamily,
    keywordstyle=\color{blue},
    commentstyle=\color{green!60!black},
    stringstyle=\color{red!80!black},
    numbers=left,
    numberstyle=\tiny\color{gray},
    frame=single,
    breaklines=true,
    tabsize=2
}

\endinput