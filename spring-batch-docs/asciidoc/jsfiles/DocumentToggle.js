$(document).ready(function(){

    // Make Java the default
    setJava();

    // Initial cookie handler. This part remembers the reader's choice and sets the toggle
    // accordingly.
    var docToggleCookieString = Cookies.get("docToggle");
    if (docToggleCookieString != null) {
        if (docToggleCookieString === "xml") {
            $("#xmlButton").prop("checked", true);
            setXml();
        } else if (docToggleCookieString === "java") {
            $("#javaButton").prop("checked", true);
            setJava();
        } else if (docToggleCookieString === "both") {
            $("#bothButton").prop("checked", true);
            setBoth();
        }
    }

    // Click handlers
    $("#xmlButton").on("click", function() {
        setXml();
    });
    $("#javaButton").on("click", function() {
        setJava();
    });
    $("#bothButton").on("click", function() {
        setBoth();
    });

    // Functions to do the work of handling the reader's choice, whether through a click
    // or through a cookie. 3652 days is 10 years, give or take a leap day.
    function setXml() {
        $("*.xmlContent").show();
        $("*.javaContent").hide();
        $("*.javaContent > *").addClass("js-toc-ignore");
        $("*.xmlContent > *").removeClass("js-toc-ignore");
        window.dispatchEvent(new Event("tocRefresh"));
        Cookies.set('docToggle', 'xml', { expires: 3652 });
    };

    function setJava() {
        $("*.javaContent").show();
        $("*.xmlContent").hide();
        $("*.xmlContent > *").addClass("js-toc-ignore");
        $("*.javaContent > *").removeClass("js-toc-ignore");
        window.dispatchEvent(new Event("tocRefresh"));
        Cookies.set('docToggle', 'java', { expires: 3652 });
    };

    function setBoth() {
        $("*.javaContent").show();
        $("*.xmlContent").show();
        $("*.javaContent > *").removeClass("js-toc-ignore");
        $("*.xmlContent > *").removeClass("js-toc-ignore");
        window.dispatchEvent(new Event("tocRefresh"));
        Cookies.set('docToggle', 'both', { expires: 3652 });
    };

});
