
# Table of Contents

1.  [Building](#orgc3ae76f)
2.  [Structure](#org6fdcbc6)
    1.  [Layers of Teal](#orgc4a76c8)
    2.  [Documentation](#org41d250a)
    3.  [Teal Intermediate Language (Teal IR)](#orgdf72828)
    4.  [Test files](#org5be69ec)
3.  [Using Teal](#org178183e)
4.  [Running Teal programs](#org534380b)
    1.  [Adding your own Teal actions](#orged7c2f0)
    2.  [Debugging Teal](#org93b743d)
        1.  [Running DrAST](#org7ef2927)
        2.  [Running code-prober](#org0125f5b)
    3.  [Internal Logging](#org893267b)
5.  [Notes on the implementation](#orgef57e5e)
6.  [Git FAQ](#org99acefd)
    1.  [What's Git?](#org29ca9c2)
    2.  [Exercise 0](#org27e72c6)
        1.  [How Do I Install the Sources on my Machine?](#org5fd8b96)
    3.  [Exercises 1 and later](#org1be4422)
        1.  [I Can't Clone the Repository](#orgdb459ad)
        2.  [How Do I Update My Fork with Changes the Instructors Made?](#org4099dbe)


<a id="orgc3ae76f"></a>

# Building

To build Teal-0 (the default) run:

    ./gradlew jar test

For other layers of Teal, set suitable parameters; e.g., for Teal-3, run:

    ~./gradlew -PlangVersion=teal-3 jar test

If you are using Teal as part of a course, you may be using a distribution of Teal that does not
include all Teal layers (possibly only Teal-0).


<a id="org6fdcbc6"></a>

# Structure

The `compiler` directory contains the implementation of the compiler.
It iss divided into three parts:

1.  `compiler/<teal-variant>/frontend`: A compiler frontend that parses code, creates an AST, runs name and type analyses (where available).
2.  `compiler/<teal-variant>/backend`: A compiler backend that takes an AST and generates Teal IR code (the intermediate representation for Teal).
3.  `ir/`: An interpreter that takes Teal IR code and executes it (via interpretation)(. All variants of Teal currently use the same interpreter.


<a id="orgc4a76c8"></a>

## Layers of Teal

Depending on your distribution, the `compiler` directory may contain multiple subdirectories,
from `compiler/teal0` to `compiler/teal3`.
Lower layers of Teal contain fewer language features.


<a id="org41d250a"></a>

## Documentation

The `docs/` directory contains a description of the Teal language
(possibly limited to your current Teal distribution).  This
definition takes precedence over the implementation; if the
implementation disagrees with the documentation, you should assume
that this is a bug in the implementation.


<a id="orgdf72828"></a>

## Teal Intermediate Language (Teal IR)

Intermediate representations are simplified representations of a programming langauge
that make it easier to analyse and/or optimise the program.
The IR tends to be more verbose than the source code, since it isn't intended for humans to read
but only for the machine to analyse and execute.

Teal IR is most closely related to register transfer IRs such as GIMPLE, LLVM bitcode, or WebAssembly


<a id="org5be69ec"></a>

## Test files

The `testfiles` directory contains test files for various parts of the compiler and interpreter.
Each file in these directories can have the following extensions:

-   `.in`: A TEAL file that is loaded in a test
-   `.expected`: The expected output of the test (not necessary TEAL, depending on what we test).
-   `.out`: The actual output we got when running the part of the compiler we're testing.
    -   Tests compare `.expected` and `.out` files.
-   `.teal`: A TEAL file that is usually imported by some other file, usually used for library code.

These files are loaded and tested by the various test classes in:

-   `compiler/teal0/test/lang`


<a id="org178183e"></a>

# Using Teal

The Teal compiler and runtime use the same entry point.  You can start
them with the following, which will print out help:
`java -jar build/teal-0.jar`

You can find this main entry point in: `teal/compiler/teal0/java/lang/Compiler.java`


<a id="org534380b"></a>

# Running Teal programs

`java -jar build/teal-0.jar program.teal --run arg1 arg2 ...`
The arguments to the program can be integers or strings. This will call the `main` function (which must
take matching formal parameters) and print out the `main` function's return value.

Try running it on  `examples/hello-world.teal` for a quick example!


<a id="orged7c2f0"></a>

## Adding your own Teal actions

You can find two predefined entry points in `teal/compiler/teal0/java/lang/Compiler.java` that you can
use to get started:

-   `Compiler.customASTAction()`, which you can run with `java -jar build/teal-0.jar program.teal -Y`, can process the AST
-   `Compiler.customIRAction()`, which you can run with `java -jar build/teal-0.jar program.teal -Z`, can process the IR


<a id="org93b743d"></a>

## Debugging Teal

To understand how Teal works or to fix a bug, you have at least four options:

-   DrAST, for interactively exploring the AST and its attributes
-   code-prober, for probing attributes from Teal source code, and for highlighting custom code
-   The Java debugger `jdb`
-   Print debugging

We recommend using DrAST or code-prober.


<a id="org7ef2927"></a>

### Running DrAST

You can start DrAST by running
`java -jar build/teal-0.jar -d <source.teal>`

On some operating systems, this may not be completely reliable; there, you can instead
run DrAST the "normal" way, following the steps from [the DrAST gitlab repository](https://bitbucket.org/jastadd/drast).


<a id="org0125f5b"></a>

### Running code-prober

To run code-prober in a POSIX environment (Linux, OS X), you can run the `codeprober.sh` script,
and then connect to code-prober with a web browser at [localhost:8080](http://localhost:8080).
You can optionally pass in a program as parameter to the `codeprober.sh` script.
To run code-prober by hand, you can manually run it with a command line similar to the following:

`java -jar libs/code-prober.jar --autoprobes nameErrors,semanticErrors,reports --syntax teal compiler/teal-0.jar -D`

or, to start with a source file already present:

`java -jar libs/code-prober.jar --autoprobes nameErrors,semanticErrors,reports --syntax teal --source <source.teal> compiler/teal-0.jar -D`

The parameter `--autoprobes` lists all attributes in the `Program` AST node from which code-prober
will extract `lang.common.Report` objects that it then shows as warning/error/info messages overlaid over
the source code.


<a id="org893267b"></a>

## Internal Logging

Can be enabled by the `TEAL_DEBUG` environment variable:

-   `export TEAL_DEBUG=interp` enables interpreter debugging
-   `export TEAL_DEBUG=irgen` enables IR generation debugging
-   `export TEAL_DEBUG=interp,irgen` enables both interpreter and IR generation debugging


<a id="orgef57e5e"></a>

# Notes on the implementation

See [the implementation notes](notes.md) (if available in your distribution).


<a id="org99acefd"></a>

# Git FAQ

Here are answers to some questions you may ask yourself when using Git,
kindly donated by Noric Couderc, the TA for 2020.


<a id="org29ca9c2"></a>

## What's Git?

Git is what's called a version control system.
But what does that mean? Let's look at each word:

-   Version: A version is a snapshot of code, it's like a picture of the state of code at a given point.
-   Control: We want to manage versions, that is, we want to do things like:
    -   Change version easily (for instance, going back to an older version)
    -   Compare two versions
    -   Merge versions together
    -   etc.
-   System: Well, that's just a program that allows you to do something, in this case, version control.

In other words, git is a piece of software that helps you track and
compare changes you (and other people!) make to your code.

Have you ever made a million changes to a program, only
to realize your idea doesn't work and now you have to get
fifteen files back to the state they were in? Well,
git's job is to make this task easy.

Git is very useful, and used *everywhere*, but it's also
a bit difficult to learn. Some git commands will seem
very mysterious as you start, and that's normal,
if you need help, please contact us!

If you want to get a rough idea of the commands, you can use this [cheat sheet](https://about.gitlab.com/images/press/git-cheat-sheet.pdf).

For a more detailed introduction, you may look at [Gitlab's documentation](https://docs.gitlab.com/ee/gitlab-basics/start-using-git.html).

Lastly, if you prefer videos with rainbows and unicorns, you may be
interested in [this series of videos by Daniel Shiffman](https://thecodingtrain.com/tracks/git-and-github-for-poets).


<a id="org27e72c6"></a>

## Exercise 0

For exercise 0, you don't need to hand in your results, so you only need to get a "clone" (i.e., a copy)
of the exercise repository onto your own machine.


<a id="org5fd8b96"></a>

### How Do I Install the Sources on my Machine?

By far the easiest approach is to use the `git clone` command.
Your favourite IDE might have built-in support for doing this for you; feel free to check its documentation!
The repository that you want is `https://git.cs.lth.se/creichen/edap15-<year>-exercise-0.git`.

1.  TL;DR

    Run the following on your favourite command shell:
    
        git clone https://git.cs.lth.se/creichen/edap15-<year>-exercise-0.git


<a id="org1be4422"></a>

## Exercises 1 and later

For exercises 1 and later, you will work together with a partner.  That means that you will
share your edits in a common repository, and use that repository as a way to submit
your solution to the teaching assistant.

Here, there are two repositories involved:

-   Your **group repository**, which we here call `origin`, that we preinitialise for you with the exercise code
-   An `upstream` repository that contains the original exercise, to which we may push changes
    if we find a bug in the Teal code that is unrelated to the exercise, or if we decide to add more documentation
    to help you with the exercise.

You will have read and write access to your `origin` **group repository**, but only read access to
the `upstream` repository.  In principle, you can solve the exercise without using the `upstream` repository,
but you may miss out on some fixes or help that we publish after the exercise goes live.


<a id="orgdb459ad"></a>

### I Can't Clone the Repository

You probably need to upload a SSH public key to the Gitlab server.
You generate those on your computer, two files will be created,
you upload the contents of of these files to the Gitlab, so it knows who you are.

The file you didn't upload (the private key) is not to be shared with anyone.

[Here](https://docs.gitlab.com/ee/ssh) is a tutorial on how to do that.


<a id="org4099dbe"></a>

### How Do I Update My Fork with Changes the Instructors Made?

Sometimes, Idriss or Christoph might update the exercises, you can synchronize your
forks with the changes have been made with git (while keeping your own changes too!).

Here's how you do it (based on [this tutorial](https://medium.com/@sahoosunilkumar/how-to-update-a-fork-in-git-95a7daadc14e)).

1.  TL;DR

    If you're too lazy to read the rest, here is the following in script form.
    Run these instructions in the `exercise-<nr>` directory::
    
        git remote add upstream https://git.cs.lth.se/creichen/edap15-<year>-exercise-<nr>.git
        git fetch upstream
        git checkout main
        git merge upstream/main
        git push origin main
    
    Otherwise, here are the explanations!

2.  List Remotes

    This gives you the list of remote repositories, they are places where code lives
    that aren't on your computer.
    
        git remote -v
    
    You should see something like
    
        origin	git@coursegit.cs.lth.se:edap15-<year>/<group>/exercise-<nr>.git (fetch)
        origin	git@coursegit.cs.lth.se:edap15-<year>/<group>/exercise-<nr>.git (push)

3.  Specify a Remote Upstream

    This is a way to tell git you know another place where similar code
    is, and that will be the address of the main exercise 1 repo, the one you forked.
    We can give names to remote, we'll call this one *upstream*.
    
        git remote add upstream https://git.cs.lth.se/creichen/edap15-exercise-<nr>.git

4.  Get the Changes

    You can get the new changes by calling the following (don't worry, it won't erase any of your code!):
    
        git fetch upstream
    
    If you look at your files, nothing should have changed. That's because
    git can handle several copies of your code simultaneously without a problem,
    using something called *branches*.
    
    So now both the code from the upstream repo and yours are on your computer
    you just can't see the other branch. You can look at it by typing `git checkout upstream/main`
    
    You can also *compare* branches with `git diff upstream/main`, this will show
    the differences between your main branch and `upstream/main`.

5.  Merging Changes

    Lastly, git is also able to merge changes from two branches together.
    There might be conflicts that you would have to resolve by hand, but in most
    cases, it works.
    
    You do this by running
    
        git checkout main # make sure you're on the right branch
        git merge upstream/main

6.  Pushing to Gitlab

    Now you can update gitlab's copy of your code with `git push origin main`

