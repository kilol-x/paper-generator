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

% 页面设置（符合老师格式：左2.5cm，右2.5cm，上2.8cm，下2.8cm）
\geometry{left=2.5cm,right=2.5cm,top=2.8cm,bottom=2.8cm}

% 超链接设置
\hypersetup{
	colorlinks=true,
	linkcolor=black,
	urlcolor=blue,
	citecolor=black
}

% ========== 修复点：替换废弃的ctexset章节配置，原生重定义章节格式 ==========
% section 一级标题：三号、加粗
\renewcommand{\section}{\@startsection{section}{1}{\z@}%
	{-3.5ex \@plus -1ex \@minus -.2ex}%
	{2.3ex \@plus.2ex}%
	{\bfseries\zihao{-3}}}

% subsection 二级标题：四号、加粗
\renewcommand{\subsection}{\@startsection{subsection}{2}{\z@}%
	{-3.25ex\@plus -1ex \@minus -.2ex}%
	{1.5ex \@plus .2ex}%
	{\bfseries\zihao{4}}}

% subsubsection 三级标题：小四号、加粗
\renewcommand{\subsubsection}{\@startsection{subsubsection}{3}{\z@}%
	{-3.25ex\@plus -1ex \@minus -.2ex}%
	{1.5ex \@plus .2ex}%
	{\bfseries\zihao{-4}}}

% 页眉页脚（老师格式：页眉显示章节名，页脚显示页码）
\pagestyle{fancy}
\fancyhf{}
\fancyhead[C]{\small \leftmark}   % 自动显示当前章节名
\fancyfoot[C]{\thepage}            % 页码居中
\renewcommand{\headrulewidth}{0.4pt}
\renewcommand{\footrulewidth}{0.4pt}
\setlength{\headheight}{12.64723pt}
\addtolength{\topmargin}{-0.64723pt}

% 目录格式（老师要求：目录包含章节编号和页码）
\RequirePackage{tocbibind}
\RequirePackage{tocloft}
\renewcommand{\cftsecfont}{\bfseries}
\renewcommand{\cftsubsecfont}{\normalfont}

% 行距（老师要求：1.25倍行距）
\setstretch{1.25}

% 首行缩进2字符
\RequirePackage{indentfirst}
\setlength{\parindent}{2em}
\setlength{\parskip}{0.3em}

% 代码块样式（符合老师格式）
\lstset{
	basicstyle=\small\ttfamily,
	keywordstyle=\color{blue},
	commentstyle=\color{green!60!black},
	stringstyle=\color{red!80!black},
	numbers=left,
	numberstyle=\tiny\color{gray},
	frame=single,
	breaklines=true,
	tabsize=2,
	showstringspaces=false,
	captionpos=b
}

% 图片、表格浮动体设置
\RequirePackage{graphicx}
\RequirePackage{caption}
\captionsetup{font=small,labelfont=bf}
\graphicspath{{../figures/}} % 统一图片路径

% 表格增强
\RequirePackage{array,booktabs}

% 脚注样式
\renewcommand{\footnoterule}{\vspace*{-3pt}\hrule width 0.4\columnwidth height 0.4pt\vspace*{2.6pt}}
\renewcommand{\thefootnote}{\arabic{footnote}}

% 边注样式
\renewcommand{\marginpar}{\vspace{0pt}\sloppy\marginparfont}
\newcommand{\marginparfont}{\small\raggedright}

% ========== 自定义全局命令（符合老师格式）==========

% 封面命令：参数1文档名称，参数2项目名称，参数3版本号
\newcommand{\doccover}[3]{
	\begin{titlepage}
		\centering
		\vspace*{3cm}
		\heiti\zihao{2}\bfseries #1 \par
		\vspace{1cm}
		\zihao{3}项目名称：#2 \par
		\vspace{2cm}
		\zihao{4}版本：#3 \par
		\vspace{3cm}
		\begin{tabular}{lc}
			成员： & 42411167 陆思雨 \\ \cline{2-2}
			成员： & 42411107 钟晶卉 \\ \cline{2-2}
			成员： & 42436015 刘洛嘉 \\ \cline{2-2}
			成员： & 42311115 郑羽婷 \\ \cline{2-2}
			成员： & 42405057 杨凯悦 \\ \cline{2-2}
			成员： & 42411052 乔鑫钰 \\ \cline{2-2}
		\end{tabular}\\[2cm]
		\vfill
		\zihao{-4}实训课程软件工程文档
	\end{titlepage}
}

% 修订历史记录环境（符合老师格式）
\newenvironment{revrecord}{
	\section*{版本修订记录}
	\begin{longtable}{|c|c|c|c|p{5cm}|}
		\hline
		版本 & 日期 & 修改人 & 审核 & 修改说明 \\
		\hline
		\endhead
	}{
	\end{longtable}
	\newpage
}

% 需求条目快捷命令
\newcommand{\reqitem}[2]{\noindent \textbf{需求#1：} #2 \par}

\endinput