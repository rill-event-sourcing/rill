bindAddButtons = ->
  $('.add-subsection').unbind()
  $('.add-subsection').bind 'click', (event) ->
    after = $(event.currentTarget).data('after')
    url = $(event.currentTarget).data('url')
    subsection_id = $(event.currentTarget).data('id')
    if subsection_id
      subsection_position = $("#subsections_" + subsection_id + "_position")[0].value
      position = parseInt(subsection_position) + 1
    else
      position = 0
    $.ajax url,
        type: 'POST'
        dataType: 'html'
        data: "position=" + position
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + after).after(data)
          bindAddButtons()
          bindDeleteButtons()
          updateViewPositions()
          refreshPreview()

bindDeleteButtons = ->
  $('.delete-subsection').unbind()
  $('.delete-subsection').bind 'click', (event) ->
    if confirm('Are you sure you want to delete this?')
      deleteItem = $(event.currentTarget).data('item')
      url = $(event.currentTarget).data('url')
      $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()
          updateViewPositions()
          refreshPreview()

bindSaveButton = ->
  $('.save').unbind()
  $('.save').bind 'click', (event) ->
    save()

save = ->
  form  = $("#section-form")
  url = form.attr("action")
  $("#edit-time").html('<img src="/spinner.gif" alt="Wait" />')
  sanitizeRichText()
  $.ajax url,
    type: 'POST'
    dataType: 'json'
    data: form.serialize()
    error: (jqXHR, textStatus, errorThrown) ->
      console.log "AJAX Error: #{ textStatus }"
    success: (data, textStatus, jqXHR) ->
      $("#edit-time").html("Saved on: " + data.updated_at)
      refreshPreview()

initializeAutoSave = ->
  setTimeout(autoSave, 10000)

autoSave = ->
  save()
  setTimeout(autoSave, 10000)

refreshPreview = ->
  $('#preview').attr("src", $('#preview').attr("src"))
  height = document.getElementById('preview').contentWindow.document.body.scrollHeight
  $('#preview').css('height', height)

updateViewPositions = ->
  $(".subsection-position").each (position) ->
    this.value = position

###########################################
# Input

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
          bindDeleteInputButtons()
          bindAddAnswerButton()
          bindCopyToClipboardButton()
          refreshPreview()

bindDeleteInputButtons = ->
  $('.delete-input').unbind()
  $('.delete-input').bind 'click', (event) ->
    if confirm('Are you sure you want to delete this?')
      deleteItem = $(event.currentTarget).data('item')
      url = $(event.currentTarget).data('url')
      $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()
          refreshPreview()

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
          list = $('#' + inputItem + ' .answers-list')
          list.append(data)
          bindDeleteAnswerButtons()
          refreshPreview()

bindDeleteAnswerButtons = ->
  $('.delete-answer').unbind()
  $('.delete-answer').bind 'click', (event) ->
    if confirm('Are you sure you want to delete this?')
      deleteItem = $(event.currentTarget).data('item')
      url = $(event.currentTarget).data('url')
      $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()
          refreshPreview()

############################
# Reflections

bindAddReflectionButton = ->
  $('#add-reflection').unbind()
  $('#add-reflection').bind 'click', (event) ->
    url = $(event.currentTarget).data('url')
    $.ajax url,
        type: 'POST'
        dataType: 'html'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#reflections-list').append(data)
          bindDeleteReflectionButtons()
          bindCopyToClipboardButton()
          refreshPreview()

bindDeleteReflectionButtons = ->
  $('.delete-reflection').unbind()
  $('.delete-reflection').bind 'click', (event) ->
    if confirm('Are you sure you want to delete this?')
      deleteItem = $(event.currentTarget).data('item')
      url = $(event.currentTarget).data('url')
      $.ajax url,
        type: 'DELETE'
        dataType: 'json'
        error: (jqXHR, textStatus, errorThrown) ->
          console.log "AJAX Error: #{ textStatus }"
        success: (data, textStatus, jqXHR) ->
          $('#' + deleteItem).remove()
          refreshPreview()

bindCopyToClipboardButton = ->
  new ZeroClipboard($(".copy-button"))

################################################################################

# on load run:
$ ->
  bindAddButtons()
  bindDeleteButtons()

  bindAddInputButton()
  bindDeleteInputButtons()
  bindAddAnswerButton()
  bindDeleteAnswerButtons()

  bindAddReflectionButton()
  bindDeleteReflectionButtons()

  bindCopyToClipboardButton()

  bindSaveButton()
  initializeAutoSave()

################################################################################
