bindAddInputButton = ->
  $('#add-input').unbind()
  $('#add-input').bind 'click', (event) ->
    inputType = $('#input-type option:selected').val()
    url = $(event.currentTarget).data('url')
    $.ajax url,
        type: 'POST'
        dataType: 'html'
        data: 'input_type=' + inputType
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#inputs-list').append(data)
          # bindAddInputButtons()
          bindDeleteInputButtons()
          bindAddAnswerButton()
          # updateCounter(star)
          # refreshPreview(star)

bindDeleteInputButtons = ->
  $('.delete-input').unbind()
  $('.delete-input').bind 'click', (event) ->
    # if confirm('Are you sure you want to delete this?')
    deleteItem = $(event.currentTarget).data('item')
    url = $(event.currentTarget).data('url')
    $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()
    #       updateCounter(star)
    #       refreshPreview(star)

bindAddAnswerButton = ->
  $('.add-answer').unbind()
  $('.add-answer').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $.ajax url,
        type: 'POST'
        dataType: 'html'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          inputItem = $(event.currentTarget).data('item')
          ul = $('#' + inputItem + ' ul')
          ul.append(data)
          bindDeleteAnswerButtons()
    #       # updateCounter(star)
    #       # refreshPreview(star)

bindDeleteAnswerButtons = ->
  $('.delete-answer').unbind()
  $('.delete-answer').bind 'click', (event) ->
    # if confirm('Are you sure you want to delete this?')
    deleteItem = $(event.currentTarget).data('item')
    url = $(event.currentTarget).data('url')
    $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()




# updateCounter = (star) ->
#   nr_of_subsections = $('.subsection-panel.star-' + star).length
#   $('#badge_' + star).html(nr_of_subsections)
#
# bindAddButtons = ->
#   $('.add-subsection').unbind()
#   $('.add-subsection').bind 'click', (event) ->
#     star = $(event.currentTarget).data('star')
#     after = $(event.currentTarget).data('after')
#     url = $(event.currentTarget).data('url')
#     $.ajax url,
#         type: 'POST'
#         dataType: 'html'
#         error: (jqXHR, textStatus, errorThrown) ->
#           console.log "AJAX Error: #{ textStatus }"
#         success: (data, textStatus, jqXHR) ->
#           $('#' + after).after(data)
#           bindAddButtons()
#           bindDeleteButtons()
#           updateCounter(star)
#           refreshPreview(star)
#
# bindDeleteButtons = ->
#   $('.delete-subsection').unbind()
#   $('.delete-subsection').bind 'click', (event) ->
#     # if confirm('Are you sure you want to delete this?')
#     deleteItem = $(event.currentTarget).data('item')
#     star = $(event.currentTarget).data('star')
#     url = $(event.currentTarget).data('url')
#     $.ajax url,
#         type: 'DELETE'
#         dataType: 'json'
#         error: (jqXHR, textStatus, errorThrown) ->
#           console.log "AJAX Error: #{ textStatus }"
#         success: (data, textStatus, jqXHR) ->
#           $('#' + deleteItem).remove()
#           updateCounter(star)
#           refreshPreview(star)
#
# bindSaveButton = ->
#   $('.save').unbind()
#   $('.save').bind 'click', (event) ->
#     save()
#
# save = ->
#   form  = $("#section-form")
#   url = form.attr("action")
#   $("#edit-time").html('<img src="/assets/spinner.gif" alt="Wait" />')
#   $.ajax url,
#     type: 'POST'
#     dataType: 'json'
#     data: form.serialize()
#     error: (jqXHR, textStatus, errorThrown) ->
#       console.log "AJAX Error: #{ textStatus }"
#     success: (data, textStatus, jqXHR) ->
#       $("#edit-time").html(data.updated_at)
#       refreshAllPreviews()
#
# refreshAllPreviews = ->
#   refreshPreview(1)
#   refreshPreview(2)
#   refreshPreview(3)
#
# refreshPreview = (star) ->
#   $('#preview-' + star).attr("src", $('#preview-' + star).attr("src"))
#   height = document.getElementById('preview-' + star).contentWindow.document.body.scrollHeight
#   $('#preview-' + star).css('height', height)

################################################################################

# on load run:
$ ->
  bindAddInputButton()
  bindDeleteInputButtons()
  bindAddAnswerButton()
  bindDeleteAnswerButtons()
  # bindSaveButton()
  # setTimeout(save, 100)
  # setInterval(save, 10000)

################################################################################
