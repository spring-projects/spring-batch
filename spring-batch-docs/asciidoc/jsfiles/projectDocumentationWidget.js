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

    Spring.buildCodeWidget(codeEl, codeWidgetEl);

    var displayValue = Cookies.get("widget.display");

    if(displayValue === 'xml') {
        $('.xml_snip_item').each(function() {
            var widget = $(this).closest('.toggleWidget');

            if(!widget.is(":hidden")) {
                $(this).trigger("click");
            }
        });
    }
    else if(displayValue === 'java') {
        $('.java_snip_item').each(function() {
            var widget = $(this).closest('.toggleWidget');

            if(!widget.is(":hidden")) {
                $(this).trigger("click");
            }
        });
    }
};

Spring.buildCodeWidget = function (codeEl, codeWidgetEl) {
    new Spring.CodeSelectorView({
        el: codeEl,
        template: $("#code-widget-controls-template").text(),
        snippetWidgetEl: codeWidgetEl
    }).render();
}

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

        if(isJavaVisible(displayVal, javaDisplayStatus)) {
            $('.javaContent').each(function() {
               $(this).show();
            });

            $('.xmlContent').each(function() {
                $(this).hide();
            });

            javaDisplayStatus = 'widget.display=java';
        } else {
            $('.xmlContent').each(function() {
                $(this).show();
            });

            $('.javaContent').each(function() {
                $(this).hide();
            });

            javaDisplayStatus = 'widget.display=xml';
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
            var activeItemOffset = 34;

            if(activeItem.data("snippet-type") === "xml") {

                if(activeItemPosition.left < 100) {
                    activeItemOffset = activeItemPosition.left - 2;
                }
                else {
                    activeItemOffset = 78;
                }
            }

            var slider = $(".js-item--slider");
            var sliderPosition = slider.position();
            var sliderOffset = sliderPosition.left;
            var sliderTarget = activeItemOffset - sliderOffset;

            slider.width(40);
            slider.css("margin-left", sliderTarget);
        }
    }

});