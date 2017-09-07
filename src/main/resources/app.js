$(function () {
    var modal = $('#modal');
    var currentStep = null;

    var steps = $('.guidebook-step').map(function() {
        var step = $(this).data('guidebook-step');
        step.journey = $(this).data('journey');
        step.screenshot = $(this).attr('href');
        return step;
    });

    function findStepsByJourney(journey) {
        return steps.filter(function() {
            return this.journey === journey;
        }).toArray();
    }

    function findStepById(id) {
        return steps.filter(function() {
            return this.id === id;
        })[0];
    }

    function setupPagination() {
        function pagItem(step, text, active, disabled) {
            var a = $('<a/>')
                .addClass('page-link')
                .html(text);
            if (step) {
                a.attr('href', step.screenshot)
                    .attr('title', step.caption)
                    .attr('guidebook-target', step.id);
            }
            a.click(function(event) {
                event.preventDefault();
                currentStep = step;
                showScreenshot();
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

        var journey = currentStep.journey;
        var stepsInJourney = findStepsByJourney(journey);

        var stepIds = stepsInJourney.map(function(s) {
            return s.id;
        });
        var idx = stepIds.indexOf(currentStep.id);

        pagination.append(pagItem(stepsInJourney[idx - 1], 'Prev', false, idx === 0));
        stepsInJourney.forEach(function(e, i) {
            pagination.append(pagItem(e, i + 1, currentStep.id === e.id, false));
        });
        pagination.append(pagItem(stepsInJourney[idx + 1], 'Next', false, idx === stepIds.length - 1));
    }

    function setupModal() {
        modal.find('.modal-title').html(currentStep.journey);
        modal.find('.modal-body figcaption').text(currentStep.caption);
        modal.find('.modal-body .badge').html(currentStep.result);
        if (currentStep.result === 'Passed') {
            modal.find('.modal-body .badge').removeClass('badge-danger').addClass('badge-success');
        } else {
            modal.find('.modal-body .badge').removeClass('badge-success').addClass('badge-danger');
        }
        setupPagination();
        setupAlerts();
    }

    function setupAlerts() {
        currentStep.alerts.forEach(function(e) {
            var icon = $('<i/>').addClass('fa').addClass('fa-exclamation-triangle');
            var a = $('<a/>')
                .addClass('guidebook-info')
                .addClass('text-warning')
                .attr('href', '#')
                .attr('title', 'Alert')
                .attr('toggle', 'popover')
                .attr('data-trigger', 'hover')
                .attr('data-placement', 'top')
                .attr('data-content', e)
                .append(icon)
                .popover();
            modal.find('.modal-body figcaption').append(a);
        });
        currentStep.notes.forEach(function(e) {
            var icon = $('<i/>').addClass('fa').addClass('fa-info-circle');
            var a = $('<a/>')
                .addClass('guidebook-info')
                .addClass('text-info')
                .attr('href', '#')
                .attr('title', 'Note')
                .attr('toggle', 'popover')
                .attr('data-trigger', 'hover')
                .attr('data-placement', 'top')
                .attr('data-content', e)
                .append(icon)
                .popover();
            modal.find('.modal-body figcaption').append(a);
        });
    }
    function showScreenshot() {
        var step = currentStep;

        modal.find('#modal-body-screenshot a').attr('href', step.screenshot);
        modal.find('#modal-body-screenshot img').attr('src', step.screenshot);

        if (step.result === 'Failed') {
            modal.find('.guidebook-modal-toggle')
                .text('[Show stacktrace]')
                .removeClass('hidden');
        } else {
            modal.find('.guidebook-modal-toggle')
                .addClass('hidden');
        }

        setupModal();
        modal.find('#modal-body-stacktrace').addClass('hidden');
        modal.find('#modal-body-screenshot').removeClass('hidden');
    }

    function showStacktrace() {
        var step = currentStep;

        modal.find('#modal-body-stacktrace code').empty();
        modal.find('#modal-body-stacktrace code').html(step.stacktrace);
        modal.find('.guidebook-modal-toggle')
            .text('[Show screenshot]')
            .removeClass('hidden');

        setupModal();
        modal.find('#modal-body-screenshot').addClass('hidden');
        modal.find('#modal-body-stacktrace').removeClass('hidden');
    }

    modal.on('show.bs.modal', function (event) {
        var target = $(event.relatedTarget);

        if (target.data('guidebook-step')) {
            currentStep = findStepById(target.data('guidebook-step').id);
        } else {
            currentStep = findStepById(target.data('guidebook-target'));
        }

        if (target.hasClass('guidebook-stacktrace')) {
            showStacktrace();
        } else {
            showScreenshot();
        }
    });

    $('.guidebook-modal-toggle').click(function(event) {
        event.preventDefault();
        var target = $(event.relatedTarget);
        if ($('#modal-body-stacktrace').hasClass('hidden')) {
            showStacktrace();
            target.text('[Show screenshot]');
        } else {
            showScreenshot();
            target.text('[Show stacktrace]');
        }
    });

    $('[data-toggle="popover"]').popover();

});