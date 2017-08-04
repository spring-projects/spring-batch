window.Spring = window.Spring || {};

var entry = 0;
/* ERB style templates conflict with Jekyll HTML escaping */
_.templateSettings = {
    evaluate    : /\{@([\s\S]+?)@\}/g,
    interpolate : /\{@=([\s\S]+?)@\}/g,
    escape      : /\{@-([\s\S]+?)@\}/g
};

Spring.ProjectDocumentationWidget = function () {
    var codeEl = $('[code-widget-controls]');
    var codeWidgetEl = $('.js-code-maven-widget');

    var projectUrl = apiBaseUrl + "/project_metadata/" + projectId;
    var promise = Spring.loadProject(projectUrl);

    promise.then(function (project) {
        Spring.buildCodeWidget(codeEl, codeWidgetEl, project);
    });
};

Spring.buildCodeWidget = function (codeEl, codeWidgetEl, project) {
    new Spring.CodeSelectorView({
        el: codeEl,
        model: project,
        template: $("#code-widget-controls-template").text(),
        snippetWidgetEl: codeWidgetEl
    }).render();
}

Spring.loadProject = function (url) {
    return $.ajax(url, {
        dataType: 'jsonp',
        processData: false
    }).then(function (value) {
        return new Spring.Project(value);
    });
}

Spring.Release = function (data) {
    _.extend(this, data);
}



Spring.Project = function (data) {
    _.extend(this, data);
    var self = this;
    this.releases = _.map(this.projectReleases, function (r) {
        return new Spring.Release(r);
    });

    return this;
};

function isJavaVisible(displayVal, currentVal) {
    var result= false;
    if(entry == 1 && (displayVal === '' || displayVal  === 'widget.display=java')) {
        result = true;
    }
    else if (entry > 1) {
        result = (currentVal  === 'java');
    }
    return result;
}

Spring.SnippetView = Backbone.View.extend({
    initialize: function () {
        var displayVal = document.cookie;
        entry++;
        var javaDisplayStatus  = this.options.snippetType;
        var codeSegmentCount = document.querySelectorAll('[id^=xml_seg_]').length +1
        for (i=1; i< codeSegmentCount ; i++) {
            var xmlSegment = document.getElementById('xml_seg_'+i);
            var javaSegment = document.getElementById('java_seg_'+i);
            if (!isJavaVisible(displayVal, this.options.snippetType)) {
                xmlSegment.style.display = 'block';
                javaSegment.style.display = 'none';
                javaDisplayStatus = 'widget.display=xml';
            } else {
                xmlSegment.style.display = 'none';
                javaSegment.style.display = 'block';
                javaDisplayStatus = 'widget.display=java';
            }
        }
        document.cookie = javaDisplayStatus;
        _.bindAll(this, "render");
    },


    remove: function() {
        this.undelegateEvents();
        this.$el.empty();
        this.unbind();
    }
});

Spring.CodeSelectorView = Backbone.View.extend({
    events: {
        "change .selector": "renderActiveWidget",
        "click .js-item": "changeCodeSource"
    },

    initialize: function () {
        this.template = _.template(this.options.template);
        this.snippetWidgetEl = this.options.snippetWidgetEl;
        _.bindAll(this, "render", "renderActiveWidget", "changeCodeSource", "_moveItemSlider", "selectCurrent");
    },

    render: function () {
        this.$el.html(
            this.template(this.model)
        );
        this.renderActiveWidget();
        this.selectCurrent();
        return this;
    },

    selectCurrent: function() {
        var selectedIndex = $('.selectpicker [data-current="true"]').val();
        if(selectedIndex == undefined) {
            selectedIndex = 0;
        }
        this.$('.selectpicker').val(selectedIndex).change();
    },

    renderActiveWidget: function() {

        if(this.activeWidget != null) this.activeWidget.remove();
        this.activeWidget = new Spring.SnippetView({
            el: this.snippetWidgetEl,
            model: this.model.releases[this.$('.selector :selected').val()],
            snippetType: this.$('.js-active').data('snippet-type')
        });
        this.activeWidget.render();

    },

    changeCodeSource: function (event) {
        var target = $(event.target);

        target.addClass("js-active");
        target.siblings().removeClass("js-active");

        this._moveItemSlider();
        this.renderActiveWidget();
    },

    _moveItemSlider: function () {
        var activeItem = $(".js-item-slider--wrapper .js-item.js-active");
        if (activeItem.length == 0) {
            return;
        } else {
            var activeItemPosition = activeItem.position();
            var activeItemOffset = activeItemPosition.left;
            var activeItemWidth = activeItem.outerWidth();

            var slider = $(".js-item--slider");
            var sliderPosition = slider.position();
            var sliderOffset = sliderPosition.left;
            var sliderTarget = activeItemOffset - sliderOffset;

            slider.width(activeItemWidth);
            slider.css("margin-left", sliderTarget);
        }
    }

});
