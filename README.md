# Building

To build Teal-0 (the default) run:

``` {.bash org-language="sh"}
./gradlew jar test
```

For other layers of Teal, set suitable parameters; e.g., for Teal-3,
run:

``` {.bash org-language="sh"}
./gradlew -PlangVersion=teal-3 jar test
```

If you are using Teal as part of a course, you may be using a
distribution of Teal that does not include all Teal layers (possibly
only Teal-0).

# Structure

The `compiler` directory contains the implementation of the compiler. It
iss divided into three parts:

1.  `compiler/<teal-variant>/frontend`: A compiler frontend that parses
    code, creates an AST, runs name and type analyses (where available).
2.  `compiler/<teal-variant>/backend`: A compiler backend that takes an
    AST and generates Teal IR code (the intermediate representation for
    Teal).
3.  `ir/`: An interpreter that takes Teal IR code and executes it (via
    interpretation)(. All variants of Teal currently use the same
    interpreter.

## Layers of Teal

Depending on your distribution, the `compiler` directory may contain
multiple subdirectories, from `compiler/teal0` to `compiler/teal3`.
Lower layers of Teal contain fewer language features.

## Documentation

The `docs/` directory contains a description of the Teal language
(possibly limited to your current Teal distribution). This definition
takes precedence over the implementation; if the implementation
disagrees with the documentation, you should assume that this is a bug
in the implementation.

## Teal Intermediate Language (Teal IR)

Intermediate representations are simplified representations of a
programming langauge that make it easier to analyse and/or optimise the
program. The IR tends to be more verbose than the source code, since it
isn\'t intended for humans to read but only for the machine to analyse
and execute.

Teal IR is most closely related to register transfer IRs such as GIMPLE,
LLVM bitcode, or WebAssembly

## Test files

The `testfiles` directory contains test files for various parts of the
compiler and interpreter. Each file in these directories can have the
following extensions:

-   `.in`: A TEAL file that is loaded in a test
-   `.expected`: The expected output of the test (not necessary TEAL,
    depending on what we test).
-   `.out`: The actual output we got when running the part of the
    compiler we\'re testing.
    -   Tests compare `.expected` and `.out` files.
-   `.teal`: A TEAL file that is usually imported by some other file,
    usually used for library code.

These files are loaded and tested by the various test classes in:

-   `compiler/teal0/test/lang`

# Using Teal

The Teal compiler and runtime use the same entry point. If you are using
Teal-0, you can start the compiler and runtime with the following, which
will print out help:

`java -jar build/teal-0.jar`

You can find this main entry point in:
`teal/compiler/teal0/java/lang/Compiler.java`

For Teal-1 and later, use `teal-1.jar` etc. instead.

# Running Teal programs

The following assume that you are using Teal-0. If you are using Teal-1
or later, replace `teal-0.jar` by `teal-1.jar`, `teal-2.jar`, or
`teal-3.jar`, as appropriate.

`java -jar build/teal-0.jar program.teal --run arg1 arg2 ...`

The arguments to the program can be integers or strings. This will call
the `main` function (which must take matching formal parameters) and
print out the `main` function\'s return value.

Try running it on `examples/hello-world.teal` for a quick example!

## Adding your own Teal actions

You can find two predefined entry points in
`teal/compiler/teal0/java/lang/Compiler.java` that you can use to get
started:

-   `Compiler.customASTAction()`, which you can run with
    `java -jar build/teal-0.jar program.teal -Y`, can process the AST
-   `Compiler.customIRAction()`, which you can run with
    `java -jar build/teal-0.jar program.teal -Z`, can process the IR

## Debugging Teal

To understand how Teal works or to fix a bug, you have at least three
options:

-   CodeProber, for interactively exploring the AST and its attributes,
    and custom queries and highlights
-   The Java debugger `jdb`
-   Print debugging

We recommend using CodeProber.

### Running CodeProber

To run CodeProber in a POSIX environment (Linux, OS X), you can run the
`code-prober.sh` script, and then connect to CodeProber with a web
browser at [localhost:8000](http://localhost:8000). You can optionally
pass in a program as parameter to the `codeprober.sh` script.

If you want to run CodeProber by hand, you can call it e.g. as:

`java -jar libs/code-prober.jar --force-syntax-highlighting=teal compiler/teal-0.jar`

Note that this will not give you the exact same setup as the
`codeprober.sh` script, which also enables some automatic probes and
enforces specific evaluation strategies; check `code-prober.sh` for
details. If you are doing a homework exercise, please avoid running
CodeProber by hand unless you know exactly what you are doing.

## Program Analyses

Teal comes with a few built-in program analyses.  Fire up CodeProber
and have a look at the attribute:

- `flowType()` for an intraprocedural (nonparametric) flow-sensitive
  type analysis
- `nullnessValue()` for an intraprocedural nullness analysis

## Internal Logging

Can be enabled by the `TEAL_DEBUG` environment variable:

-   `export TEAL_DEBUG=interp` enables interpreter debugging
-   `export TEAL_DEBUG=irgen` enables IR generation debugging
-   `export TEAL_DEBUG=interp,irgen` enables both interpreter and IR
    generation debugging

# Notes on the implementation

See [the implementation notes](notes.org) (if available in your
distribution).

# Git FAQ

Here are answers to some questions you may ask yourself when using Git,
kindly donated by Noric Couderc, the TA for 2020.

## What\'s Git?

Git is what\'s called a version control system. But what does that mean?
Let\'s look at each word:

-   Version: A version is a snapshot of code, it\'s like a picture of
    the state of code at a given point.
-   Control: We want to manage versions, that is, we want to do things
    like:
    -   Change version easily (for instance, going back to an older
        version)
    -   Compare two versions
    -   Merge versions together
    -   etc.
-   System: Well, that\'s just a program that allows you to do
    something, in this case, version control.

In other words, git is a piece of software that helps you track and
compare changes you (and other people!) make to your code.

Have you ever made a million changes to a program, only to realize your
idea doesn\'t work and now you have to get fifteen files back to the
state they were in? Well, git\'s job is to make this task easy.

Git is very useful, and used *everywhere*, but it\'s also a bit
difficult to learn. Some git commands will seem very mysterious as you
start, and that\'s normal, if you need help, please contact us!

If you want to get a rough idea of the commands, you can use this [cheat
sheet](https://about.gitlab.com/images/press/git-cheat-sheet.pdf).

For a more detailed introduction, you may look at [Gitlab\'s
documentation](https://docs.gitlab.com/ee/gitlab-basics/start-using-git.html).

Lastly, if you prefer videos with rainbows and unicorns, you may be
interested in [this series of videos by Daniel
Shiffman](https://thecodingtrain.com/tracks/git-and-github-for-poets).

## Exercise 0

For exercise 0, you don\'t need to hand in your results, so you only
need to get a \"clone\" (i.e., a copy) of the exercise repository onto
your own machine.

### How Do I Install the Sources on my Machine?

By far the easiest approach is to use the `git clone` command. Your
favourite IDE might have built-in support for doing this for you; feel
free to check its documentation! The repository that you want is
`https://git.cs.lth.se/creichen/edap15-<year>-exercise-0.git`.

1.  TL;DR

    Run the following on your favourite command shell:

    ``` {.bash org-language="sh"}
    git clone https://git.cs.lth.se/creichen/edap15-<year>-exercise-0.git
    ```

## Exercises 1 and later

For exercises 1 and later, you will work together with a partner. That
means that you will share your edits in a common repository, and use
that repository as a way to submit your solution to the teaching
assistant.

Here, there are two repositories involved:

-   Your **group repository**, which we here call `origin`, that we
    preinitialise for you with the exercise code
-   An `upstream` repository that contains the original exercise, to
    which we may push changes if we find a bug in the Teal code that is
    unrelated to the exercise, or if we decide to add more documentation
    to help you with the exercise.

You will have read and write access to your `origin` **group
repository**, but only read access to the `upstream` repository. In
principle, you can solve the exercise without using the `upstream`
repository, but you may miss out on some fixes or help that we publish
after the exercise goes live.

### I Can\'t Clone the Repository

You probably need to upload a SSH public key to the Gitlab server. You
generate those on your computer, two files will be created, you upload
the contents of of these files to the Gitlab, so it knows who you are.

The file you didn\'t upload (the private key) is not to be shared with
anyone.

[Here](https://docs.gitlab.com/ee/ssh) is a tutorial on how to do that.

### How Do I Update My Fork with Changes the Instructors Made?

Sometimes, Idriss or Christoph might update the exercises, you can
synchronize your forks with the changes have been made with git (while
keeping your own changes too!).

Here\'s how you do it (based on [this
tutorial](https://medium.com/@sahoosunilkumar/how-to-update-a-fork-in-git-95a7daadc14e)).

1.  TL;DR

    If you\'re too lazy to read the rest, here is the following in
    script form. Run these instructions in the `exercise-<nr>`
    directory::

    ``` {.bash org-language="sh"}
    git remote add upstream https://git.cs.lth.se/creichen/edap15-<year>-exercise-<nr>.git
    git fetch upstream
    git checkout main
    git merge upstream/main
    git push origin main
    ```

    Otherwise, here are the explanations!

2.  List Remotes

    This gives you the list of remote repositories, they are places
    where code lives that aren\'t on your computer.

    ``` {.bash org-language="sh"}
    git remote -v
    ```

    You should see something like

    ``` text
    origin    git@coursegit.cs.lth.se:edap15-<year>/<group>/exercise-<nr>.git (fetch)
    origin    git@coursegit.cs.lth.se:edap15-<year>/<group>/exercise-<nr>.git (push)
    ```

3.  Specify a Remote Upstream

    This is a way to tell git you know another place where similar code
    is, and that will be the address of the main exercise 1 repo, the
    one you forked. We can give names to remote, we\'ll call this one
    *upstream*.

    ``` {.bash org-language="sh"}
    git remote add upstream https://git.cs.lth.se/creichen/edap15-exercise-<nr>.git
    ```

4.  Get the Changes

    You can get the new changes by calling the following (don\'t worry,
    it won\'t erase any of your code!):

    ``` {.bash org-language="sh"}
    git fetch upstream
    ```

    If you look at your files, nothing should have changed. That\'s
    because git can handle several copies of your code simultaneously
    without a problem, using something called *branches*.

    So now both the code from the upstream repo and yours are on your
    computer you just can\'t see the other branch. You can look at it by
    typing `git checkout upstream/main`

    You can also *compare* branches with `git diff upstream/main`, this
    will show the differences between your main branch and
    `upstream/main`.

5.  Merging Changes

    Lastly, git is also able to merge changes from two branches
    together. There might be conflicts that you would have to resolve by
    hand, but in most cases, it works.

    You do this by running

    ``` {.bash org-language="sh"}
    git checkout main # make sure you're on the right branch
    git merge upstream/main
    ```

6.  Pushing to Gitlab

    Now you can update gitlab\'s copy of your code with
    `git push origin main`
