// FORM TYPES -------------------------------------------------------

function checkbox(form_id, question, other, required, choices) {
  var form = FormApp.openById(form_id)
  var item = form.addCheckboxItem()
    .setTitle(question)
    .showOtherOption(other)
    .setRequired(required)
    .setChoiceValues(choices)
  return item.getId()
}

function checkboxImgs(form_id, question, other, required, choices, urls) {
  var item_id = checkbox(form_id, question, other, required, choices)
  choiceImgs(FormApp.openById(form_id), choices, urls)
  return item_id
}

function estimation(form_id, question, required) {
  var form = FormApp.openById(form_id)
  var item = form.addTextItem()
    .setTitle(question)
    .setRequired(required)
  validateNum(form_id, item.getId(), "Please enter a number")
  return item.getId()
}

function multiEstimation(form_id, question, fields, required, dim) {
  FormApp.openById(form_id).addSectionHeaderItem()
    .setTitle(question)
  var item1 = estimation(form_id, fields[0], required)
  for (var i = 1; i < dim; i++) {
    estimation(form_id, fields[i], required)
  }
  return item1
}

function freeText(form_id, question, required) {
  var form = FormApp.openById(form_id)
  var item = form.addParagraphTextItem()
    .setTitle(question)
    .setRequired(required)
  return item.getId()
}

function radioButton(form_id, question, other, required, choices) {
  var form = FormApp.openById(form_id)
  var item = form.addMultipleChoiceItem()
    .setTitle(question)
    .showOtherOption(other)
    .setRequired(required)
    .setChoiceValues(choices)
  return item.getId()
}

function radioButtonImgs(form_id, question, other, required, choices, urls) {
  var item_id = radioButton(form_id, question, other, required, choices)
  choiceImgs(FormApp.openById(form_id), choices, urls)
  return item_id
}

// FORM ADD-ONS -------------------------------------------------------

function addImage(form_id, url, title, help_text) {
  var img = UrlFetchApp.fetch(url)
  FormApp.openById(form_id).addImageItem()
    .setTitle(title)
    .setHelpText(help_text) // the image description
    .setImage(img)
}

function choiceImgs(form, choices, urls) {
  for(var i = 0; i < choices.length; i++){
    var img = UrlFetchApp.fetch(urls[i])
    form.addImageItem()
      .setImage(img)
      .setTitle(choices[i])
  }
}

function setDescription(form_id, text) {
  FormApp.openById(form_id).setDescription(text)
}

function setConfirmation(form_id, text) {
  FormApp.openById(form_id).setConfirmationMessage(text)
}

function setShuffle(form_id) {
  FormApp.openById(form_id).setShuffleQuestions(true)
}

function validateNum(form_id, item_id, help_text) {
  var validation = FormApp.createTextValidation()
    .setHelpText(help_text)
    .requireNumber()
    .build()
  getTextItem(form_id, item_id).setValidation(validation)
}

function validateInt(form_id, item_id, help_text) {
  var validation = FormApp.createTextValidation()
    .setHelpText(help_text)
    .requireWholeNumber()
    .build()
  getTextItem(form_id, item_id).setValidation(validation)
}

function validateGreater(form_id, item_id, help_text, min) {
  var validation = FormApp.createTextValidation()
    .setHelpText(help_text)
    .requireNumberGreaterThan(min)
    .build()
  getTextItem(form_id, item_id).setValidation(validation)
}

function validateLess(form_id, item_id, help_text, max) {
  var validation = FormApp.createTextValidation()
    .setHelpText(help_text)
    .requireNumberLessThan(max)
    .build()
  getTextItem(form_id, item_id).setValidation(validation)
}

function validateRange(form_id, item_id, help_text, min, max) {
  var validation = FormApp.createTextValidation()
    .setHelpText(help_text)
    .requireNumberBetween(min, max) // inclusive
    .build()
  getTextItem(form_id, item_id).setValidation(validation)
}

function shuffleForm(form_id) {
  var form = FormApp.openById(form_id)
  var items = form.getItems()

  for(var i = 0; i < items.length; i++) {
    if (items[i].getType() == FormApp.ItemType.MULTIPLE_CHOICE) {
      var item = items[i].asMultipleChoiceItem()
      shuffleQuestion(item)
   } else if (items[i].getType() == FormApp.ItemType.MULTIPLE_CHOICE)
       var item = items[i].asMultipleChoiceItem()
       shuffleQuestion(item)
   }
}

function shuffleQuestion(item) {
  var choices = item.getChoices();
  var newChoices = new Array(choices.length);

  for(var j = 0; j < choices.length; j++) {
    newChoices[j] = choices[j].getValue();
  }
  shuffle(newChoices);
  item.setChoiceValues(newChoices)
}

// UTILITIES ----------------------------------------------------------

function addForm(title, limit) {
  var form = FormApp.create(title)
  form.setLimitOneResponsePerUser(limit)
  return form.getId()
}

function getEditUrl(id) {
  var form = FormApp.openById(id)
  return form.getEditUrl()
}

function getPublishedUrl(id) {
  var form = FormApp.openById(id)
  return form.getPublishedUrl()
}

function getFormResponses(id) {
  var form = FormApp.openById(id)
  var formResponses = form.getResponses()
  var responseArr = new Array(formResponses.length)
  var idx = 0

  for(var i = 0; i < formResponses.length; i++) {
    var itemResponses = formResponses[i].getItemResponses()
    for (var j = 0; j < itemResponses.length; j++) {
      responseArr[idx++] = itemResponses[j].getResponse()
    }
 }
  return responseArr
}

function getItemResponses(id, item_id, index) {
  var form = FormApp.openById(id)
  var formResponses = form.getResponses()
  var responseArr = new Array(formResponses.length)
  var found = false
  var idx = 0

  if (formResponses.length == index) return [] // no new responses yet
  for (var i = index; i < formResponses.length; i++) {
    var itemResponses = formResponses[i].getItemResponses()
    for (var j = 0; j < itemResponses.length; j++) {
      if (item_id == itemResponses[j].getItem().getId()) {
        found = true
        responseArr[idx++] = itemResponses[j].getResponse()
      }
    }
  }
  if (found) return responseArr
  else return ["Question not found"]
}

function getMultiResponses(form_id, item_id, index, dim) {
  var form = FormApp.openById(form_id)
  var formResponses = form.getResponses()
  var responseArr = new Array(formResponses.length)
  var found = false
  var idx = 0

  if (formResponses.length == index) return [] // no new responses yet
  for (var i = index; i < formResponses.length; i++) {
    var itemResponses = formResponses[i].getItemResponses()
    for (var j = 0; j < itemResponses.length; j++) {
      if (item_id == itemResponses[j].getItem().getId()) {
        found = true
        var response = new Array(dim)
        for (var k = 0; k < dim; k++) {
          response[k] = itemResponses[j++].getResponse()
        }
        responseArr[idx++] = response
      }
    }
  }
  if (found) return responseArr
  else return ["Question not found"]
}

function getTextItem(form_id, item_id) {
  var form = FormApp.openById(form_id)
  return form.getItemById(item_id).asTextItem()
}

// https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
function shuffle(a) {
  var j, x, i
  for (i = a.length - 1; i > 0; i--) {
    j = Math.floor(Math.random() * (i + 1))
    x = a[i]
    a[i] = a[j]
    a[j] = x
  }
  return a
}
