$(function () {
    $('#modal').on('show.bs.modal', function (event) {
        var modal = $(this);
        var target = $(event.relatedTarget);
        modal.find(".modal-body").each(function() {
            $(this).addClass("hidden");
        });
        modal.find('.modal-title').html(target.attr("title"));
        if (target.hasClass("cause")) {
            modal.find('#modal-body-cause pre code').empty();
            modal.find('#modal-body-cause pre code').html(target.data('cause'));
            modal.find('#modal-body-cause').removeClass("hidden");
        } else if (target.hasClass("screenshot")) {
            modal.find('#modal-body-screenshot a').attr("href", target.attr("href"));
            modal.find('#modal-body-screenshot img').attr("src", target.attr("href"));
            modal.find('#modal-body-screenshot figcaption').html(target.attr("title"));
            modal.find('#modal-body-screenshot').removeClass("hidden");
        } else {
            modal.find('#modal-body-message').empty();
            modal.find('#modal-body-message').html(target.data('info'));
            modal.find('#modal-body-message').removeClass("hidden");
        }
    })
});