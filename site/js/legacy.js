//Scripts.js
//Loads all sorts of random JS functions and instantiates various items site-wide
//Sliders, modals (fancybox), PPC campaign tracking, etc.

/*
Hortonworks JS Application "class". Goal is to cleanup and 
migrate all those random functions into this class.
*/
var HWX = (function() {

	var $ = jQuery,
		menuItem = $('#fat-nav .menu-item'),
		loginMenuItem = $('.open[data-tool="profile"]'),
		menuContainer = $('.menu-header-navigation-container'),
		menuItemID;

	// Retrive query string keys such as "login"
	qs = function( key ) {
		key = key.replace(/[*+?^$.\[\]{}()|\\\/]/g, "\\$&"); // escape RegEx meta chars
		var match = location.search.match(new RegExp("[?&]"+key+"=([^&]+)(&|$)"));
		return match && decodeURIComponent(match[1].replace(/\+/g, " "));
	};

	disableMenu = function() {
		menuItem.off( 'mouseenter').off('mouseleave');
		menuItem.addClass('disabled');
	};

	enableMenu = function() {
		menuItem.on( 'mouseenter', function showSubMenu() {
			var prevMenuID = menuItemID;
			menuItemID = $(this).attr('id');
			$('#fat-nav #'+menuItemID+' .sub-menu').fadeIn(100, function(){
				$('.tools-list .icon-profile').removeClass('hover');
				$('.tools-list .sub-menu').fadeOut(50);
				if (prevMenuID && menuItemID != prevMenuID && $('#fat-nav #'+prevMenuID+' .sub-menu').length ) {
					$('#fat-nav #'+prevMenuID+' .sub-menu').fadeOut(50);
				}
			});
		});
		menuContainer.on( 'mouseleave', function hideSubMenu() {
			$('#fat-nav .sub-menu').fadeOut(100);
			$('.tools-list .icon-profile').removeClass('hover');
			$('.tools-list .sub-menu').fadeOut(50);
			menuItemID = false;
		});
		menuItem.removeClass('disabled');

		loginMenuItem.on( 'mouseenter', function showLoginMenu() {
			$('#fat-nav .sub-menu').fadeOut(50);
			$('.tools-list .icon-profile').addClass('hover');
			$( '.sub-menu', $(this).closest('li') ).fadeIn(50);
		});
	};

	return {

		init: function() {
			this.setMenuEvents();
			this.mobileMenuToggle();
			this.tabs();
			this.miscHelpers();
		},

		setMenuEvents: function() {
			enableMenu();
		},

		mobileMenuToggle: function() {
			$('.menu-link').on( 'click', function() {
				$(this).toggleClass('active');
				$('#navbar, .mainbody').toggleClass('active');
				return false;
			});
		},


		tabs: function() {

			// ================================================================================================
			// Tab System - Used to delineate sections as the HDP Pages, Sandbox Pages etc.
			// ================================================================================================
			function activateTab( $tab, $firstload ) {
				// First check to see if the tab system is in place!
				if ( $('.sectionnav.auto ul li').length === 0 ) {
					return false;
				}

				if ( window.location.hash && $firstload ) {

					$(window).load( function() {
						$(this).scrollTop(0);
					});

					$tab = $('.sectionnav.auto ul li a[href="' + window.location.hash + '"]').parent();
					console.log(window.location.hash);

					if ( $tab.length < 1 ) {
						$childtab = $('ul.tabs a[href="' + window.location.hash + '"]').parent();
						parentid = $childtab.closest( '.panel' ).attr('id');

						$tab = $('.sectionnav.auto ul li a[href="#' + parentid + '"]').parent();
					}
					
				}

				// $tab expects a jquery object in the sectionnav
				if ( $tab === void(0) || $tab.length < 1 ) {
					// On page load, there's no object, so choose the first tab.
					$tab = $('.sectionnav ul li:first-child');
				}

				var $activeTab = $tab.closest('ul').find('li.active'),
					contentLocation = $tab.children('a').attr("href");

				// Strip off the current url that IE adds
				contentLocation = contentLocation.replace(/^.+#/, '#');

				//Make Tab Active
				$activeTab.removeClass('active');
				$tab.addClass('active');

				//Show Tab Content
				$(contentLocation).closest('.panels').children('.panel').removeClass('active').hide();
				$(contentLocation).css('display', 'block').addClass('active');

			}
			activateTab( void(0), true );

			// Set up event Handlers for Tabs
			$( document ).on( 'click', '.sectionnav.auto li a', function ( event ) {
				event.preventDefault();
				history.pushState( null, null, $(this).attr('href') );
				activateTab( $( this ).parent('li'), false );
			});

			$('.innernav a').on('click', function(evt) {
				// Used for the Back/Forward buttons inside page sections, such as the tutorials.
				event.preventDefault();
				$('html, body').animate({ scrollTop: 0 }, 'fast');
				var next = $(this).attr('data-target');
				var newtab = $('.sectionnav.auto ul li a[href="#section_' + next + '"]').parent();
				activateTab(newtab, false);
;
			});

			$('.in-page-anchor').on('click', function(evt) {
				// Used for in-page anchor links that are not in the left nav (tabs).
				evt.preventDefault();
				var next = $(this).attr('href');
				var newtab = $('.sectionnav.auto ul li a[href="' + next + '"]').parent();
				history.pushState( null, null, $(this).attr('href') );
				activateTab(newtab, false);
				;
			});
		},


		miscHelpers: function() {

			// Add External link icon to regular links that are external.
			$('.mainbody a').each(function() {
				var url = $(this).attr('href');
				if( url && url.indexOf('hortonworks.com') == -1 &&
					url.indexOf('hortonworks.dev') == -1 &&
					url.indexOf('/') !== 0 &&
					url.indexOf('#') !== 0 &&
					this.children.length === 0 &&
					this.className === "") {
						$(this).addClass('ext-link');
						$(this).attr('target', '_blank');
				}
			});

			// If page contains Gravity Forms complex elements, move the labels above the fields. - This is noticable on the Partner Page.
			$(".ginput_complex span").each(function(i,e){
				var $label = $(e).find("label");
				var $field = $(e).find("input");
				var label_value = $label.text();
				$field.attr('placeholder', label_value);
				$label.remove();
			});

			// Use when linking to an #anchor on a page to smooth scroll to it
			$('.smoothscroll').on( 'click', function( e ) {
				e.preventDefault();

				var target = $(this).attr('href');

				$('html,body').animate({ scrollTop: $(target).offset().top }, 500);
			});

			// Disable submit if terms and conditions textarea is present
			// if ( $('#terms_conditions').length > 0 ) {
			// 	var form = $('#terms_conditions').closest( 'form' );

			// 	$( 'input[type="submit"]', form ).prop( 'disabled', true );
			// 	$( 'input[type="submit"]', form ).prop( 'title', 'Please Scroll Terms and Conditions' );
			// 	$( 'input[type="submit"]', form ).addClass( 'disabled' );
			// }

			// Enable submit after scrolling whole terms and conditions textarea
			// $('#terms_conditions').scroll( function() {
			// 	var form = $(this).closest( 'form' );

			// 	if ( $(this).scrollTop() + $(this).height() >= $(this)[0].scrollHeight - 500 ) {
			// 		$( 'input[type="submit"]', form ).prop( 'disabled', false );
			// 		$( 'input[type="submit"]', form ).removeClass( 'disabled' );
			// 	}
			// });

			// This is used with the product licenses page
			$('.license_fancybox').on( 'click', function() {
				var license = $( 'span', this);
				$.fancybox({
					content		: $(license).html(),
					wrapCSS		: 'roundedcorners',
					maxHeight	: 600,
					type		: 'inline',
					padding		: '30'
				});

				return false;
			});

			//$('.fancybox-media').fancybox(); // Used for the Partner side-bar links
		},

	};

} () );


jQuery(document).ready(function() {
	HWX.init();
});



/* ================================================================================================
 * EVALUATE EVERYTHING BELOW THIS LINE FOR PRUNING
 *
 * ================================================================================================ */


jQuery(document).ready( function($) {
	//colapse + expand download buttons
	$('ul.group_parent li li a').fadeOut(0);
	$('ul.group_parent li a.group_parent_a').bind('click', function(e){
		e.preventDefault();
		$(this).toggleClass('active_parent');
		$(this).parent('li').find('a').not('.group_parent_a').fadeToggle(300);
	});

	// Adds a class of 'ext-link' to any external link
	function addClass(url, object) {
		if( url.indexOf('hortonworks.com') == -1 &&
			url.indexOf('www.hortonworks.com') == -1 &&
			url.indexOf('hortonworks.dev') == -1 &&
			url.indexOf('localhost') == -1 &&
			url.indexOf('#') !== 0 &&
			url.indexOf('http') === 0 &&
			object.children.length === 0
		) {
			$( object ).addClass('ext-link');
			$( object ).attr( 'target', '_blank' );
		}
	}

	// I don't know why this is here?
	$('body').addClass('js');

	var cloakEmails = function() {
		// Take emails in the format of : <a href="#" class="cloak-email" data-before="sales-emaea" data-after="hortonworks.com"></a>
		$('a.cloak-email').each(function() {
			var after = $(this).data("after");
			var before = $(this).data("before");
			$(this).attr('href', 'mailto:' + before + '@' + after);
			if(!($(this).text())) {
				$(this).text(before + "@" + after);
			}
		});
	}();

	// Draw attention to responses from forms that were submitted
	$('.response.fadein').fadeIn();

	// Cornify Easter Egg.... for shits and giggles.... only works on James Dev
	var konami_keys = [38, 38, 40, 40, 37, 39, 37, 39];
	var konami_index = 0;
	$(document).keydown(function(e){
		if(e.keyCode === konami_keys[konami_index++]){
			if(konami_index === konami_keys.length){
				$(document).unbind('keydown', arguments.callee);
				$.getScript('/wp-content/themes/hortonworks/js/libs/cornify.js', function() {
					cornify_add();
					$(document).keydown(cornify_add);
				});
			}
		}else{
			konami_index = 0;
		}
	});




});
