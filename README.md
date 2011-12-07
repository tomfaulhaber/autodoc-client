This project lets you manipulate autodoc data that's been pushed to a github gh-pages branch.

Autodoc (more info [here](http:/tomfaulhaber.github.com/autodoc)) produces Clojure formatted index files that contain all the information that is in the HTML files in a way that can be accessed directly by a Clojure program.

This library is a convenience library to allow programmers to pull the information directly from github repositories and use it to build whatever they want withoutout having to load the library whose documentation they want to access.

To get the documentation with sources just do:

    (use 'autodoc-client.retrieve)

    (def docs (add-source (retrieve "https://github.com/clojure/clojure" "v1.3")))

Note here that `retrieve` loads the index file from github (see [clojure/index-v1.3.clj](https://github.com/clojure/clojure/blob/gh-pages/index-v1.3.clj) for an example) and `add-source` pulls the source data referenced from the var entries into a :source-text key in the map for that var.

You can also get a list of versions that are documented for a repo:

    (versions "https://github.com/clojure/clojure")
    => ("v1.1" "v1.2" "v1.3")