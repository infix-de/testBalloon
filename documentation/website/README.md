## TestBalloon documentation website

This website is created with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).

### Installation

Follow the Material for MkDocs instructions for [installing into a Python virtual environment with pip](https://squidfunk.github.io/mkdocs-material/getting-started#with-pip).

### Local usage

* Update the API reference: `gradlew dokkaGenerateHtml`
* Serve the website locally: `gradlew documentationWebsiteServiceStart`
* Stop serving the website: `gradlew documentationWebsiteServiceStop`
* Build the dev website locally: `gradlew documentationWebsiteUpdate --quiet`
* Build the release website locally: `gradlew documentationWebsiteUpdate -Plocal.documentation.useProjectVersion=true --quiet`

* Run a shell session in the virtual environment: `source venv/bin/activate`
    * Serve the website locally: `mkdocs serve`
    * Serve the website on IP address ADDR: `mkdocs serve --dev-addr ADDR:8000`
    * Build the website locally: `mkdocs build`

### Reference

* [Reference](https://squidfunk.github.io/mkdocs-material/reference/) – admonitions, annotations, code blocks, tabs, diagrams
    * [Admonitions](https://squidfunk.github.io/mkdocs-material/reference/admonitions#admonitions)
    * [Annotations](https://squidfunk.github.io/mkdocs-material/reference/annotations#annotations)
* [Advanced configuration](https://squidfunk.github.io/mkdocs-material/getting-started#with-pip) – colors, fonts, navigation, search, versioning
* [Publishing](https://squidfunk.github.io/mkdocs-material/publishing-your-site/)
* [Emojipedia](https://emojipedia.org/) 
* [Markdown extensions](https://squidfunk.github.io/mkdocs-material/setup/extensions/python-markdown-extensions/)
    * [Highlight](https://squidfunk.github.io/mkdocs-material/setup/extensions/python-markdown-extensions#highlight)
    * [Snippets](https://facelessuser.github.io/pymdown-extensions/extensions/snippets/)
    * [SuperFences](https://facelessuser.github.io/pymdown-extensions/extensions/superfences/)
        * [Highlighting Lines](https://facelessuser.github.io/pymdown-extensions/extensions/superfences#highlighting-lines)
    * [toc – Table of Contents](https://github.com/Python-Markdown/markdown/blob/master/docs/extensions/toc.md)
* [Mermaid Flowcharts](https://mermaid.js.org/syntax/flowchart.html)
