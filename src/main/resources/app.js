$(function () {
    var screenshots = $('.screenshot');

    function findStepsByJourney(journey) {
        return screenshots.filter(function() {
            var j = $(this).data('journey');
            return j === journey;
        }).toArray();
    }

    function setupPagination(currentStep, modal) {
        function pagItem(step, text, active, disabled) {
            var a = $('<a/>')
                .addClass('page-link')
                .html(text);
            if (step) {
                a.attr('href', $(step).attr('href'))
                    .attr('title', $(step).attr('title'))
                    .attr('data-step', $(step).data('step'))
                    .attr('data-journey', $(step).data('journey'));
            }
            a.click(function(event) {
                event.preventDefault();
                showScreenshot(a, modal);
            });
            var li = $('<li/>')
                .addClass('page-item')
                .append(a);
            if (disabled) {
                li.addClass('disabled');
            }
            if (active) {
                li.addClass('active');
            }
            return li;
        }

        var pagination = modal.find('.pagination');
        pagination.empty();

        var journey = currentStep.data('journey');
        var stepsInJourney = findStepsByJourney(journey);

        var stepNames = stepsInJourney.map(function(s) {
            return $(s).data('step');
        });
        var currentStepName = $(currentStep).data('step');
        var idx = stepNames.indexOf(currentStepName);

        pagination.append(pagItem(stepsInJourney[idx - 1], 'Prev', false, idx === 0));
        stepsInJourney.forEach(function(e, i) {
            pagination.append(pagItem(e, i + 1, currentStepName === $(e).data('step'), false));
        });
        pagination.append(pagItem(stepsInJourney[idx + 1], 'Next', false, idx === stepNames.length - 1));
    }

    function showScreenshot(el, modal) {
        var journey = el.data('journey');

        modal.find('#modal-body-screenshot a').attr('href', el.attr('href'));
        modal.find('#modal-body-screenshot img').attr('src', el.attr('href'));
        modal.find('#modal-body-screenshot figcaption').html(el.attr('title'));
        modal.find('.modal-title').html(journey);

        setupPagination(el, modal);

        modal.find('#modal-body-screenshot').removeClass('hidden');
        modal.find('.modal-footer').removeClass('hidden');
        return false;
    }

    $('#modal').on('show.bs.modal', function (event) {
        var modal = $(this);
        var target = $(event.relatedTarget);
        modal.find('.modal-body').each(function() {
            $(this).addClass('hidden');
        });
        modal.find('.modal-footer').each(function() {
            $(this).addClass('hidden');
        });
        modal.find('.modal-title').html(target.attr('title'));
        if (target.hasClass('cause')) {
            modal.find('#modal-body-cause pre code').empty();
            modal.find('#modal-body-cause pre code').html(target.data('cause'));
            modal.find('#modal-body-cause').removeClass('hidden');
        } else if (target.hasClass('screenshot')) {
            showScreenshot(target, modal);
        } else {
            modal.find('#modal-body-message').empty();
            modal.find('#modal-body-message').html(target.data('info'));
            modal.find('#modal-body-message').removeClass('hidden');
        }
    })
});