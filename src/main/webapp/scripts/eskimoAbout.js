
if (typeof eskimo === "undefined" || eskimo == null) {
    window.eskimo = {}
}
eskimo.About = function() {

    const that = this;

    this.initialize = function() {
        // Initialize HTML Div from Template
        $("#about-modal-wrapper").load("html/eskimoAbout.html", function (responseTxt, statusTxt, jqXHR) {

            if (statusTxt === "success") {

                $("#about-modal-header-close").click(() => { cancelAbout(); });
                $("#about-modal-button-close").click(() => { cancelAbout(); });

            } else if (statusTxt === "error") {
                alert("Error: " + jqXHR.status + " " + jqXHR.statusText);
            }
        });
    };

    function showAbout () {

        $.ajaxGet({
            timeout: 1000 * 10,
            url: "fetch-about-info",
            success: (data, status, jqXHR) => {

                if (!data || data.error) {
                    console.error(data.error);
                    eskimoMain.alert(ESKIMO_ALERT_LEVEL.ERROR, data.error);

                } else {

                    $("#about-eskimo-version").html(data["about-eskimo-version"]);

                    $("#about-eskimo-build-timestamp").html(data["about-eskimo-build-timestamp"]);
                    $("#about-eskimo-runtime-timestamp").html(data["about-eskimo-runtime-timestamp"]);
                    $("#about-eskimo-kube-enabled").html("" + data["about-eskimo-kube-enabled"]);
                    $("#about-eskimo-demo-mode").html("" + data["about-eskimo-demo-mode"]);
                    $("#about-eskimo-packages-url").html(data["about-eskimo-packages-url"]);

                    $("#about-eskimo-java-home").html(data["about-eskimo-java-home"]);
                    $("#about-eskimo-java-version").html(data["about-eskimo-java-version"]);
                    $("#about-eskimo-working-dir").html(data["about-eskimo-working-dir"]);
                    $("#about-eskimo-os-name").html(data["about-eskimo-os-name"]);
                    $("#about-eskimo-os-version").html(data["about-eskimo-os-version"]);

                    $('#about-modal').modal("show");
                }
            }
        });
    }
    this.showAbout = showAbout;

    function cancelAbout() {
        $('#about-modal').modal("hide");
    }
    this.cancelAbout = cancelAbout;
};