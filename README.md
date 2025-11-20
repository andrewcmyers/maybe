An inplementation of the Option pattern for Java, improving on the builtin Optional class in various ways.

* The method names are more intuitive: e.g.,  `then` instead of `map`
* The exception that is raised when an empty Maybe is used is a checked exception so you don't forget to handle it. It is also a fast exception so it doesn't create much performance overhead.
* It implements the `Set` interface, so it can be accessed with a `for`-loop.

Documentation here: [https://andrewcmyers.github.io/maybe/cms/util/maybe/package-summary.html]
