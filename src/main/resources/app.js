$(function () {
    $('#modal').on('show.bs.modal', function (event) {
        var modal = $(this);
        var target = $(event.relatedTarget);
        if (target.hasClass("cause")) {
            modal.find('.modal-title').html(target.attr("title"));
            modal.find('#modal-body-cause pre code').empty();
            modal.find('#modal-body-cause pre code').html(target.data('cause'));
            modal.find('#modal-body-cause').removeClass("hidden");
            modal.find('#modal-body-other').addClass("hidden");
        } else if (target.hasClass("screenshot")) {
            var img = $("<img/>")
                .attr("src", target.attr("href"));
            var link = $("<a/>")
                .attr("href", target.attr("href"))
                .attr("parent", "_blank")
                .append(img);
            modal.find('.modal-title').html(target.attr('title'));
            modal.find('#modal-body-other').empty();
            modal.find('#modal-body-other').append(link);
            modal.find('#modal-body-cause').addClass("hidden");
            modal.find('#modal-body-other').removeClass("hidden");
        } else {
            modal.find('.modal-title').html(target.attr('title'));
            modal.find('#modal-body-other').empty();
            modal.find('#modal-body-other').html(target.data('info'));
            modal.find('#modal-body-cause').addClass("hidden");
            modal.find('#modal-body-other').removeClass("hidden");
        }
    })
});