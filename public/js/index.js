
var mode = "home";

$('#home').click(function () {
  if (mode != "home") {
    deleteTimeline();
    createLoader();
    mode = "home";
    setButtonToSelected('home');
    setTimeout(function() {
      getTimelineJSON(function(data) {
        deleteTimeline();
        createTimeline(data);
      });
    }, 500);
  }
});

$('#reply').click(function () {
  if (mode != "reply") {
    deleteTimeline();
    createLoader();
    mode = "reply";
    setButtonToSelected('reply');
    setTimeout(function() {
      getReplyJSON(function(data) {
        deleteTimeline();
        createTimeline(data);
      });
    }, 500);
  }
});

var setButtonToSelected = function (button) {
  var buttonList = {
    'home':{notselected: 'assets/img/button_home.png', selected: 'assets/img/button_selected_home.png'},
    'reply':{notselected: 'assets/img/button_reply.png', selected: 'assets/img/button_selected_reply.png'}
  };
  var menuImgs = $('.menu-img');
  for (var i = 0; i < menuImgs.length; i++) {
    $('a > img', menuImgs[i]).attr('src', buttonList[menuImgs[i].id].notselected);
  }
  $('.menu-img#' + button + ' > a > img').attr('src', buttonList[button].selected);
}


var createTweet = function(tweet, user) {
  return $('<section></section>', {addClass: 'timeline-tweet'})
    .append(
      $('<div></div>', {addClass: 'tweet-main'})
        .append(
          $('<div></div>', {addClass: 'tweet-img'})
          .append(
            $('<a></a>', {href: '/' + user.screen_name})
            .append(
              $('<img>', {src: '/assets/' + user.profile_image_url, alt: user.name + "'s icon"})
            )
          )
        )
        .append(
          $('<footer></footer>', {addClass: 'tweet-footer'})
          .append(
            $('<div></div>', {addClass: 'tweet-username'})
              .text(user.name)
          )
          .append(
            $('<div></div>', {addClass: 'tweet-screenname'})
              .text(tweet.screen_name)
          )
          .append(
            $('<div></div>', {addClass: 'tweet-diffdate'})
              .text(tweet.created_at)
          )
        )
        .append(
          $('<p></p>', {addClass: 'tweet-text'})
            .text(tweet.text)
        )
    );
}

var createLoader = function () {
  $('#timeline').append(
    $('<div></div>', {addClass: 'loading'})
      .append($('<img>', {src: '/assets/img/load.gif', alt: 'loading...'}))
    );
}

var getTimelineJSON = function (callback) {
  $.ajax({
    typw: 'GET',
    url: '/api/timeline',
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};

var getReplyJSON = function (callback) {
  $.ajax({
    typw: 'GET',
    url: '/api/reply',
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};


var createTimeline = function (data) {
  for (var i = 0; i < data.length; i++) {
    var domTweet = createTweet(data[i].tweet, data[i].user)
    $('#timeline').append(domTweet);
  }
}

var deleteTimeline = function () {
  $('#timeline').html('');
}

getTimelineJSON(function(data) {
  createTimeline(data);
});


