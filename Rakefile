# frozen_string_literal: true

require 'English'
$LOAD_PATH.unshift File.expand_path('.')

require 'rake'
require 'net/telnet'
require 'stringio'
require 'fileutils'
require 'open-uri'

include Rake::DSL

Rake.application.instance_variable_set(:@name, 'go')
orig_verbose = verbose
verbose(false)

# The CrazyFun build grammar. There's no magic here, just ruby
require 'rake_tasks/crazy_fun/main'
require 'rake_tasks/selenium_rake/detonating_handler'
require 'rake_tasks/selenium_rake/crazy_fun'

# The CrazyFun builders - Most of these are either partially or fully obsolete
# Note the order here is important - The top 2 are used in inheritance chains
require 'rake_tasks/crazy_fun/mappings/file_copy_hack'
require 'rake_tasks/crazy_fun/mappings/tasks'
require 'rake_tasks/crazy_fun/mappings/rake_mappings'

# Location of all new (non-CrazyFun) methods
require 'rake_tasks/selenium_rake/browsers'
require 'rake_tasks/selenium_rake/checks'
require 'rake_tasks/selenium_rake/cpp_formatter'
require 'rake_tasks/selenium_rake/ie_generator'
require 'rake_tasks/selenium_rake/java_formatter'
require 'rake_tasks/selenium_rake/type_definitions_generator'

# Our modifications to the Rake / Bazel libraries
require 'rake/task'
require 'rake_tasks/rake/task'
require 'rake_tasks/rake/dsl'
require 'rake_tasks/bazel/task'

# These are the final items mixed into the global NS
# These need moving into correct namespaces, and not be globally included
require 'rake_tasks/bazel'
require 'rake_tasks/copyright'
require 'rake_tasks/python'

$DEBUG = orig_verbose != Rake::FileUtilsExt::DEFAULT
$DEBUG = true if ENV['debug'] == 'true'

verbose($DEBUG)

def java_version
  File.foreach('java/version.bzl') do |line|
    return line.split('=').last.strip.tr('"', '') if line.include?('SE_VERSION')
  end
end

# The build system used by webdriver is layered on top of rake, and we call it
# "crazy fun" for no readily apparent reason.

# First off, create a new CrazyFun object.
crazy_fun = SeleniumRake::CrazyFun.new

# Secondly, we add the handlers, which are responsible for turning a build
# rule into a (series of) rake tasks. For example if we're looking at a file
# in subdirectory "subdir" contains the line:
#
# java_library(:name => "example", :srcs => ["foo.java"])
#
# we would generate a rake target of "//subdir:example" which would generate
# a Java JAR at "build/subdir/example.jar".
#
# If crazy fun doesn't know how to handle a particular output type ("java_library"
# in the example above) then it will throw an exception, stopping the build
CrazyFun::Mappings::RakeMappings.new.add_all(crazy_fun)

# Finally, find every file named "build.desc" in the project, and generate
# rake tasks from them. These tasks are normal rake tasks, and can be invoked
# from rake.
# FIXME: the rules for the targets were removed and build files won't load
# crazy_fun.create_tasks(Dir['**/build.desc'])

# If it looks like a bazel target, build it with bazel
rule(%r{//.*}) do |task|
  task.out = Bazel.execute('build', %w[], task.name)
end

# Spoof tasks to get CI working with bazel
task '//java/test/org/openqa/selenium/environment/webserver:webserver:uber' => [
  '//java/test/org/openqa/selenium/environment:webserver'
]

# Java targets required for release. These should all be java_export targets.
# Generated from: bazel query 'kind(maven_publish, set(//java/... //third_party/...))' | sort
JAVA_RELEASE_TARGETS = %w[
  //java/src/org/openqa/selenium/chrome:chrome.publish
  //java/src/org/openqa/selenium/chromium:chromium.publish
  //java/src/org/openqa/selenium/devtools/v119:v119.publish
  //java/src/org/openqa/selenium/devtools/v120:v120.publish
  //java/src/org/openqa/selenium/devtools/v118:v118.publish
  //java/src/org/openqa/selenium/devtools/v85:v85.publish
  //java/src/org/openqa/selenium/edge:edge.publish
  //java/src/org/openqa/selenium/firefox:firefox.publish
  //java/src/org/openqa/selenium/grid/sessionmap/jdbc:jdbc.publish
  //java/src/org/openqa/selenium/grid/sessionmap/redis:redis.publish
  //java/src/org/openqa/selenium/grid:bom-dependencies.publish
  //java/src/org/openqa/selenium/grid:bom.publish
  //java/src/org/openqa/selenium/grid:grid.publish
  //java/src/org/openqa/selenium/ie:ie.publish
  //java/src/org/openqa/selenium/json:json.publish
  //java/src/org/openqa/selenium/manager:manager.publish
  //java/src/org/openqa/selenium/os:os.publish
  //java/src/org/openqa/selenium/remote/http:http.publish
  //java/src/org/openqa/selenium/remote:remote.publish
  //java/src/org/openqa/selenium/safari:safari.publish
  //java/src/org/openqa/selenium/support:support.publish
  //java/src/org/openqa/selenium:client-combined.publish
  //java/src/org/openqa/selenium:core.publish
].freeze

# Notice that because we're using rake, anything you can do in a normal rake
# build can also be done here. For example, here we set the default task
task default: [:grid]

task all: [
  :'selenium-java',
  '//java/test/org/openqa/selenium/environment:webserver'
]

task tests: [
  '//java/test/org/openqa/selenium/htmlunit:htmlunit',
  '//java/test/org/openqa/selenium/firefox:test-synthesized',
  '//java/test/org/openqa/selenium/ie:ie',
  '//java/test/org/openqa/selenium/chrome:chrome',
  '//java/test/org/openqa/selenium/edge:edge',
  '//java/test/org/openqa/selenium/support:small-tests',
  '//java/test/org/openqa/selenium/support:large-tests',
  '//java/test/org/openqa/selenium/remote:small-tests',
  '//java/test/org/openqa/selenium/remote/server/log:test',
  '//java/test/org/openqa/selenium/remote/server:small-tests'
]
task chrome: ['//java/src/org/openqa/selenium/chrome']
task grid: [:'selenium-server-standalone']
task ie: ['//java/src/org/openqa/selenium/ie']
task firefox: ['//java/src/org/openqa/selenium/firefox']
task remote: %i[remote_server remote_client]
task remote_client: ['//java/src/org/openqa/selenium/remote']
task remote_server: ['//java/src/org/openqa/selenium/remote/server']
task safari: ['//java/src/org/openqa/selenium/safari']
task selenium: ['//java/src/org/openqa/selenium:core']
task support: ['//java/src/org/openqa/selenium/support']

desc 'Build the standalone server'
task 'selenium-server-standalone' => '//java/src/org/openqa/selenium/grid:executable-grid'

task test_javascript: [
  '//javascript/atoms:test-chrome:run',
  '//javascript/webdriver:test-chrome:run',
  '//javascript/selenium-atoms:test-chrome:run',
  '//javascript/selenium-core:test-chrome:run'
]
task test_chrome: ['//java/test/org/openqa/selenium/chrome:chrome:run']
task test_edge: ['//java/test/org/openqa/selenium/edge:edge:run']
task test_chrome_atoms: [
  '//javascript/atoms:test-chrome:run',
  '//javascript/chrome-driver:test-chrome:run',
  '//javascript/webdriver:test-chrome:run'
]
task test_htmlunit: [
  '//java/test/org/openqa/selenium/htmlunit:htmlunit:run'
]
task test_grid: [
  '//java/test/org/openqa/grid/common:common:run',
  '//java/test/org/openqa/grid:grid:run',
  '//java/test/org/openqa/grid/e2e:e2e:run',
  '//java/test/org/openqa/selenium/remote:remote-driver-grid-tests:run'
]
task test_ie: [
  '//cpp/iedriverserver:win32',
  '//cpp/iedriverserver:x64',
  '//java/test/org/openqa/selenium/ie:ie:run'
]
task test_jobbie: [:test_ie]
task test_firefox: ['//java/test/org/openqa/selenium/firefox:marionette:run']
task test_remote_server: [
  '//java/test/org/openqa/selenium/remote/server:small-tests:run',
  '//java/test/org/openqa/selenium/remote/server/log:test:run'
]
task test_remote: [
  '//java/test/org/openqa/selenium/json:small-tests:run',
  '//java/test/org/openqa/selenium/remote:common-tests:run',
  '//java/test/org/openqa/selenium/remote:client-tests:run',
  '//java/test/org/openqa/selenium/remote:remote-driver-tests:run',
  :test_remote_server
]
task test_safari: ['//java/test/org/openqa/selenium/safari:safari:run']
task test_support: [
  '//java/test/org/openqa/selenium/support:small-tests:run',
  '//java/test/org/openqa/selenium/support:large-tests:run'
]

task :test_java_webdriver do
  if SeleniumRake::Checks.windows?
    Rake::Task['test_ie'].invoke
  elsif SeleniumRake::Checks.chrome?
    Rake::Task['test_chrome'].invoke
  elsif SeleniumRake::Checks.edge?
    Rake::Task['test_edge'].invoke
  else
    Rake::Task['test_htmlunit'].invoke
    Rake::Task['test_firefox'].invoke
    Rake::Task['test_remote_server'].invoke
  end
end

task test_java: [
  '//java/test/org/openqa/selenium/atoms:test:run',
  :test_java_small_tests,
  :test_support,
  :test_java_webdriver,
  :test_selenium,
  'test_grid'
]

task test_java_small_tests: [
  '//java/test/org/openqa/selenium:small-tests:run',
  '//java/test/org/openqa/selenium/json:small-tests:run',
  '//java/test/org/openqa/selenium/support:small-tests:run',
  '//java/test/org/openqa/selenium/remote:common-tests:run',
  '//java/test/org/openqa/selenium/remote:client-tests:run',
  '//java/test/org/openqa/grid/selenium/node:node:run',
  '//java/test/org/openqa/grid/selenium/proxy:proxy:run',
  '//java/test/org/openqa/selenium/remote/server:small-tests:run',
  '//java/test/org/openqa/selenium/remote/server/log:test:run'
]

task :test do
  if SeleniumRake::Checks.python?
    Rake::Task['test_py'].invoke
  else
    Rake::Task['test_javascript'].invoke
    Rake::Task['test_java'].invoke
  end
end

task test_py: [:py_prep_for_install_release, 'py:marionette_test']
task build: %i[all firefox remote selenium tests]

desc 'Clean build artifacts.'
task :clean do
  rm_rf 'build/'
  rm_rf 'java/build/'
  rm_rf 'dist/'
end

# Create a new IEGenerator instance
ie_generator = SeleniumRake::IEGenerator.new

# Generate a C++ Header file for mapping between magic numbers and #defines
# in the C++ code.
ie_generator.generate_type_mapping(
  name: 'ie_result_type_cpp',
  src: 'cpp/iedriver/result_types.txt',
  type: 'cpp',
  out: 'cpp/iedriver/IEReturnTypes.h'
)

desc 'Generate Javadocs'
task javadocs: %i[//java/src/org/openqa/selenium/grid:all-javadocs] do
  rm_rf 'build/docs/api/java'
  mkdir_p 'build/docs/api/java'

  out = 'bazel-bin/java/src/org/openqa/selenium/grid/all-javadocs.jar'

  cmd = %(cd build/docs/api/java && jar xf "../../../../#{out}" 2>&1)
  cmd = cmd.tr('/', '\\').tr(':', ';') if SeleniumRake::Checks.windows?

  ok = system(cmd)
  ok or raise 'could not unpack javadocs'

  File.open('build/docs/api/java/stylesheet.css', 'a') do |file|
    file.write(<<~STYLE
      /* Custom selenium-specific styling */
      .blink {
        animation: 2s cubic-bezier(0.5, 0, 0.85, 0.85) infinite blink;
      }

      @keyframes blink {
        50% {
          opacity: 0;
        }
      }

    STYLE
              )
  end
end

file 'cpp/iedriver/sizzle.h' => ['//third_party/js/sizzle:sizzle:header'] do
  cp 'build/third_party/js/sizzle/sizzle.h', 'cpp/iedriver/sizzle.h'
end

task sizzle_header: ['cpp/iedriver/sizzle.h']

task ios_driver: [
  '//javascript/atoms/fragments:get_visible_text:ios',
  '//javascript/atoms/fragments:click:ios',
  '//javascript/atoms/fragments:back:ios',
  '//javascript/atoms/fragments:forward:ios',
  '//javascript/atoms/fragments:submit:ios',
  '//javascript/atoms/fragments:xpath:ios',
  '//javascript/atoms/fragments:xpaths:ios',
  '//javascript/atoms/fragments:type:ios',
  '//javascript/atoms/fragments:get_attribute:ios',
  '//javascript/atoms/fragments:clear:ios',
  '//javascript/atoms/fragments:is_selected:ios',
  '//javascript/atoms/fragments:is_enabled:ios',
  '//javascript/atoms/fragments:is_shown:ios',
  '//javascript/atoms/fragments:stringify:ios',
  '//javascript/atoms/fragments:link_text:ios',
  '//javascript/atoms/fragments:link_texts:ios',
  '//javascript/atoms/fragments:partial_link_text:ios',
  '//javascript/atoms/fragments:partial_link_texts:ios',
  '//javascript/atoms/fragments:get_interactable_size:ios',
  '//javascript/atoms/fragments:scroll_into_view:ios',
  '//javascript/atoms/fragments:get_effective_style:ios',
  '//javascript/atoms/fragments:get_element_size:ios',
  '//javascript/webdriver/atoms/fragments:get_location_in_view:ios'
]

desc 'Create zipped assets for Java for uploading to GitHub'
task :'java-release-zip' do
  Bazel.execute('build', ['--stamp'], '//java/src/org/openqa/selenium:client-zip')
  Bazel.execute('build', ['--stamp'], '//java/src/org/openqa/selenium/grid:server-zip')
  Bazel.execute('build', ['--stamp'], '//java/src/org/openqa/selenium/grid:executable-grid')
  mkdir_p 'build/dist'
  FileUtils.rm_f('build/dist/*.{server,java}*')

  FileUtils.copy('bazel-bin/java/src/org/openqa/selenium/grid/server-zip.zip',
                 "build/dist/selenium-server-#{java_version}.zip")
  FileUtils.chmod(666, "build/dist/selenium-server-#{java_version}.zip")
  FileUtils.copy('bazel-bin/java/src/org/openqa/selenium/client-zip.zip',
                 "build/dist/selenium-java-#{java_version}.zip")
  FileUtils.chmod(666, "build/dist/selenium-java-#{java_version}.zip")
  FileUtils.copy('bazel-bin/java/src/org/openqa/selenium/grid/selenium',
                 "build/dist/selenium-server-#{java_version}.jar")
  FileUtils.chmod(777, "build/dist/selenium-server-#{java_version}.jar")
end

task 'release-java': %i[java-release-zip publish-maven]

def read_m2_user_pass
  # First check env vars, then the settings.xml config inside .m2
  user = nil
  pass = nil
  if ENV['SEL_M2_USER'] && ENV['SEL_M2_PASS']
    puts 'Fetching m2 user and pass from environment variables.'
    user = ENV['SEL_M2_USER']
    pass = ENV['SEL_M2_PASS']
    return [user, pass]
  end
  settings = File.read("#{Dir.home}/.m2/settings.xml")
  found_section = false
  settings.each_line do |line|
    if !found_section
      found_section = line.include? '<id>sonatype-nexus-staging</id>'
    elsif user.nil? && line.include?('<username>')
      user = line.split('<username>')[1].split('</')[0]
    elsif pass.nil? && line.include?('<password>')
      pass = line.split('<password>')[1].split('</')[0]
    end
  end

  [user, pass]
end

desc 'Publish all Java jars to Maven as stable release'
task 'publish-maven': JAVA_RELEASE_TARGETS do
  creds = read_m2_user_pass
  JAVA_RELEASE_TARGETS.each do |p|
    Bazel.execute('run',
                  ['--stamp',
                   '--define',
                   'maven_repo=https://oss.sonatype.org/service/local/staging/deploy/maven2',
                   '--define',
                   "maven_user=#{creds[0]}",
                   '--define',
                   "maven_password=#{creds[1]}",
                   '--define',
                   'gpg_sign=true'],
                  p)
  end
end

desc 'Publish all Java jars to Maven as nightly release'
task 'publish-maven-snapshot': JAVA_RELEASE_TARGETS do
  creds = read_m2_user_pass
  if java_version.end_with?('-SNAPSHOT')
    JAVA_RELEASE_TARGETS.each do |p|
      Bazel.execute('run',
                    ['--stamp',
                     '--define',
                     'maven_repo=https://oss.sonatype.org/content/repositories/snapshots',
                     '--define',
                     "maven_user=#{creds[0]}",
                     '--define',
                     "maven_password=#{creds[1]}",
                     '--define',
                     'gpg_sign=false'],
                    p)
    end
  else
    puts 'No SNAPSHOT version configured. Targets will not be pushed to the snapshot repo in SonaType.'
  end
end

desc 'Install jars to local m2 directory'
task :'maven-install' do
  JAVA_RELEASE_TARGETS.each do |p|
    Bazel.execute('run',
                  ['--stamp',
                   '--define',
                   "maven_repo=file://#{Dir.home}/.m2/repository",
                   '--define',
                   'gpg_sign=false'],
                  p)
  end
end

desc 'Build the selenium client jars'
task 'selenium-java' => '//java/src/org/openqa/selenium:client-combined'

desc 'Update AUTHORS file'
task :authors do
  sh "(git log --use-mailmap --format='%aN <%aE>' ; cat .OLD_AUTHORS) | sort -uf > AUTHORS"
end

namespace :copyright do
  desc 'Update Copyright notices on all files in repo'
  task :update do
    Copyright.new.update(
      FileList['javascript/**/*.js'].exclude(
        'javascript/atoms/test/jquery.min.js',
        'javascript/jsunit/**/*.js',
        'javascript/node/selenium-webdriver/node_modules/**/*.js',
        'javascript/selenium-core/lib/**/*.js',
        'javascript/selenium-core/scripts/ui-element.js',
        'javascript/selenium-core/scripts/ui-map-sample.js',
        'javascript/selenium-core/scripts/user-extensions.js',
        'javascript/selenium-core/scripts/xmlextras.js',
        'javascript/selenium-core/xpath/**/*.js',
        'javascript/grid-ui/node_modules/**/*.js'
      )
    )
    Copyright.new.update(FileList['javascript/**/*.tsx'])
    Copyright.new(comment_characters: '#').update(FileList['py/**/*.py'].exclude(
                                                    'py/selenium/webdriver/common/bidi/cdp.py',
                                                    'py/generate.py',
                                                    'py/selenium/webdriver/common/devtools/**/*',
                                                    'py/venv/**/*'
                                                  ))
    Copyright.new(comment_characters: '#', prefix: ["# frozen_string_literal: true\n", "\n"])
             .update(FileList['rb/**/*.rb'])
    Copyright.new.update(FileList['java/**/*.java'])
    Copyright.new.update(FileList['rust/**/*.rs'])

    sh './scripts/format.sh'
  end
end

namespace :side do
  task atoms: [
    '//javascript/atoms/fragments:find-element'
  ] do
    # TODO: move directly to IDE's directory once the repositories are merged
    mkdir_p 'build/javascript/atoms'

    atom = 'bazel-bin/javascript/atoms/fragments/find-element.js'
    name = File.basename(atom)

    puts "Generating #{atom} as #{name}"
    File.open(File.join(baseDir, name), 'w') do |f|
      f << "// GENERATED CODE - DO NOT EDIT\n"
      f << 'module.exports = '
      f << File.read(atom).strip
      f << ";\n"
    end
  end
end

def node_version
  File.foreach('javascript/node/selenium-webdriver/package.json') do |line|
    return line.split(':').last.strip.tr('",', '') if line.include?('version')
  end
end
namespace :node do
  atom_list = %w[
    //javascript/atoms/fragments:find-elements
    //javascript/atoms/fragments:is-displayed
    //javascript/webdriver/atoms:get-attribute
  ]

  task atoms: atom_list do
    base_dir = 'javascript/node/selenium-webdriver/lib/atoms'
    mkdir_p base_dir

    ['bazel-bin/javascript/atoms/fragments/is-displayed.js',
     'bazel-bin/javascript/webdriver/atoms/get-attribute.js',
     'bazel-bin/javascript/atoms/fragments/find-elements.js'].each do |atom|
      name = File.basename(atom)
      puts "Generating #{atom} as #{name}"
      File.open(File.join(base_dir, name), 'w') do |f|
        f << "// GENERATED CODE - DO NOT EDIT\n"
        f << 'module.exports = '
        f << File.read(atom).strip
        f << ";\n"
      end
    end
  end

  desc 'Build Node npm package'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//javascript/node/selenium-webdriver')
  end

  task :'dry-run' do
    Bazel.execute('run', ['--stamp'], '//javascript/node/selenium-webdriver:selenium-webdriver.pack')
  end

  desc 'Release Node npm package'
  task :release do
    Bazel.execute('run', ['--stamp'], '//javascript/node/selenium-webdriver:selenium-webdriver.publish')
  end

  desc 'Release Node npm package'
  task deploy: :release

  desc 'Update Node version'
  task :version, [:version] do |_task, arguments|
    old_version = node_version
    new_version = updated_version(old_version, arguments[:version])

    file = 'javascript/node/selenium-webdriver/package.json'
    text = File.read(file).gsub(old_version, new_version)
    File.open(file, "w") { |f| f.puts text }
  end
end

def python_version
  File.foreach('py/BUILD.bazel') do |line|
    return line.split('=').last.strip.tr('"', '') if line.include?('SE_VERSION')
  end
end
namespace :py do
  desc 'Build Python wheel and sdist with optional arguments'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//py:selenium-wheel')
    Bazel.execute('build', args, '//py:selenium-sdist')
  end

  desc 'Release Python wheel and sdist to pypi'
  task :release, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Rake::Task['py:build'].invoke(args)
    sh "python3 -m twine upload `bazel-bin/py/selenium`-#{python_version}-py3-none-any.whl"
    sh "python3 -m twine upload bazel-bin/py/selenium-#{python_version}.tar.gz"
  end

  desc 'Update generated Python files for local development'
  task :update do
    Bazel.execute('build', [], '//py:selenium')

    FileUtils.rm_rf('py/selenium/webdriver/common/devtools/')
    FileUtils.cp_r('bazel-bin/py/selenium/webdriver/.', 'py/selenium/webdriver', remove_destination: true)
  end

  desc 'Generate Python documentation'
  task :docs do
    FileUtils.rm_rf('build/docs/api/py/')
    FileUtils.rm_rf('build/docs/doctrees/')
    begin
      sh 'tox -c py/tox.ini -e docs', verbose: true
    rescue StandardError
      puts 'Ensure that tox is installed on your system'
      raise
    end
  end

  desc 'Install Python wheel locally'
  task :install do
    Bazel.execute('build', [], '//py:selenium-wheel')
    begin
      sh 'pip install bazel-bin/py/selenium-*.whl'
    rescue StandardError
      puts 'Ensure that Python and pip are installed on your system'
      raise
    end
  end

  desc 'Update Python version'
  task :version, [:version] do |_task, arguments|
    old_version = python_version
    new_version = updated_version(old_version, arguments[:version])

    ['py/setup.py',
     'py/BUILD.bazel',
     'py/selenium/__init__.py',
     'py/selenium/webdriver/__init__.py',
     'py/docs/source/conf.py'].each do |file|
        text = File.read(file).gsub(old_version, new_version)
        File.open(file, "w") { |f| f.puts text }
    end
  end
end

def ruby_version
  File.foreach('rb/lib/selenium/webdriver/version.rb') do |line|
    return line.split('=').last.strip.tr("'", '') if line.include?('VERSION')
  end
end
namespace :rb do
  desc 'Generate Ruby gems'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//rb:selenium-webdriver')
    Bazel.execute('build', args, '//rb:selenium-devtools')
  end

  desc 'Update generated Ruby files for local development'
  task :update do
    Bazel.execute('build', [], '@bundle//:bundle')
    Rake::Task['rb:build'].invoke
    Rake::Task['grid'].invoke
  end

  desc 'Push Ruby gems to rubygems'
  task :release, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Bazel.execute('run', args, '//rb:selenium-webdriver')
    Bazel.execute('run', args, '//rb:selenium-devtools')
  end

  desc 'Generate Ruby documentation'
  task :docs do
    FileUtils.rm_rf('build/docs/api/rb/')
    Bazel.execute('run', [], '//rb:docs')
    FileUtils.cp_r('bazel-bin/rb/docs.rb.sh.runfiles/selenium/docs/api/rb/.', 'build/docs/api/rb')
  end

  desc 'Update Ruby version'
  task :version, [:version] do |_task, arguments|
    old_version = ruby_version
    new_version = updated_version(old_version, arguments[:version])
    new_version += '.nightly' unless old_version.include?('nightly')

    file = 'rb/lib/selenium/webdriver/version.rb'
    text = File.read(file).gsub(old_version, new_version)
    File.open(file, "w") { |f| f.puts text }
  end
end

def dotnet_version
  File.foreach('dotnet/selenium-dotnet-version.bzl') do |line|
    return line.split('=').last.strip.tr('"', '') if line.include?('SE_VERSION')
  end
end
namespace :dotnet do
  desc 'Build nupkg files'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//dotnet:all')
  end

  desc 'Create zipped assets for .NET for uploading to GitHub'
  task :zip_assets, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Rake::Task['dotnet:build'].invoke(args)
    mkdir_p 'build/dist'
    FileUtils.rm_f('build/dist/*dotnet*')

    FileUtils.copy('bazel-bin/dotnet/release.zip', "build/dist/selenium-dotnet-#{dotnet_version}.zip")
    FileUtils.chmod(666, "build/dist/selenium-dotnet-#{dotnet_version}.zip")
    FileUtils.copy('bazel-bin/dotnet/strongnamed.zip', "build/dist/selenium-dotnet-strongnamed-#{dotnet_version}.zip")
    FileUtils.chmod(666, "build/dist/selenium-dotnet-strongnamed-#{dotnet_version}.zip")
  end

  desc 'Upload nupkg files to Nuget'
  task :release, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Rake::Task['dotnet:build'].invoke(args)
    Rake::Task['dotnet:zip_assets'].invoke(args)

    ["./bazel-bin/dotnet/src/webdriver/Selenium.WebDriver.#{dotnet_version}.nupkg",
     "./bazel-bin/dotnet/src/support/Selenium.Support.#{dotnet_version}.nupkg"].each do |asset|
      sh "dotnet nuget push #{asset} --api-key #{ENV.fetch('NUGET_API_KEY', nil)} --source https://api.nuget.org/v3/index.json"
    end
  end

  desc 'Generate .NET documentation'
  task :docs do
    begin
      sh 'dotnet tool update -g docfx'
    rescue StandardError
      puts 'Please ensure that .NET SDK is installed.'
      raise
    end

    begin
      sh 'docfx dotnet/docs/docfx.json'
    rescue StandardError
      case $CHILD_STATUS.exitstatus
      when 127
        raise 'Ensure the dotnet/tools directory is added to your PATH environment variable (e.g., `~/.dotnet/tools`)'
      when 255
        puts 'Build failed, likely because of DevTools namespacing. This is ok; continuing'
      else
        raise
      end
    end
  end

  desc 'Update .NET version'
  task :version, [:version] do |_task, arguments|
    old_version = dotnet_version
    new_version = updated_version(old_version, arguments[:version])

    file = 'dotnet/selenium-dotnet-version.bzl'
    text = File.read(file).gsub(old_version, new_version)
    File.open(file, "w") { |f| f.puts text }
  end
end

namespace :java do
  desc 'Build Java Client Jars'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//java/src/org/openqa/selenium:client-combined')
  end

  desc 'Build Grid Jar'
  task :grid, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//java/src/org/openqa/selenium/grid:grid')
  end

  desc 'Package Java bindings and grid into releasable packages'
  task :package, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Rake::Task['java:build'].invoke(args)
    Rake::Task['java-release-zip'].invoke
  end

  desc 'Deploy all jars to Maven'
  task :release, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Rake::Task['java:package'].invoke(args)
    Rake::Task['publish-maven'].invoke
  end

  desc 'Install jars to local m2 directory'
  task install: :'maven-install'

  desc 'Generate Java documentation'
  task docs: :javadocs

  desc 'Update Maven dependencies'
  task :update do
    file_path = 'java/maven_deps.bzl'
    content = File.read(file_path)
    # For some reason ./go wrapper is not outputting from Open3, so cannot use Bazel class directly
    output = `bazel run @maven//:outdated`

    output.scan(/\S+ \[\S+-alpha\]/).each do |match|
      puts "WARNING — Cannot automatically update alpha version of: #{match}"
    end

    versions = output.scan(/(\S+) \[\S+ -> (\S+)\]/).to_h
    versions.each do |artifact, version|
      if artifact.match?('graphql')
        puts "WARNING — Cannot automatically update graphql"
        next
      end

      replacement = artifact.include?('googlejavaformat') ? "#{artifact}:jar:#{version}" : "#{artifact}:#{version}"
      content.gsub!(/#{artifact}:(jar:)?\d+\.\d+[^\\"]+/, replacement)
    end
    File.write(file_path, content)

    args = ['--action_env=RULES_JVM_EXTERNAL_REPIN=1']
    Bazel.execute('run', args, '@unpinned_maven//:pin')
  end

  desc 'Update Java version'
  task :version, [:version] do |_task, arguments|
    old_version = java_version
    new_version = updated_version(old_version, arguments[:version])
    new_version += '-SNAPSHOT' unless old_version.include?('SNAPSHOT')

    file = 'java/version.bzl'
    text = File.read(file).gsub(old_version, new_version)
    File.open(file, "w") { |f| f.puts text }
  end
end

def rust_version
  File.foreach('rust/BUILD.bazel') do |line|
    return line.split('=').last.strip.tr('",', '') if line.include?('version =')
  end
end
namespace :rust do
  desc 'Build Selenium Manager'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Bazel.execute('build', args, '//rust:selenium-manager')
  end

  desc 'Update the rust lock files'
  task :update do
    sh 'CARGO_BAZEL_REPIN=true bazel sync --only=crates'
  end

  desc 'Update Rust version'
  task :version, [:version] do |_task, arguments|
    puts rust_version
    puts rust_version.split('.')
    puts rust_version.split('.').tap(&:shift).join('.')
    old_version = arguments[:version] ? arguments[:version] : rust_version.split('.').tap(&:shift).join('.')
    new_version = updated_version(old_version, arguments[:version])
    new_version = new_version.split('.').tap(&:pop).join('.')

    ['rust/Cargo.toml', 'rust/BUILD.bazel'].each do |file|
      text = File.read(file).gsub(old_version, new_version)
      File.open(file, "w") { |f| f.puts text }
    end
  end
end

namespace :all do
  desc 'Update all API Documentation'
  task :docs do
    Rake::Task['java:docs'].invoke
    Rake::Task['py:docs'].invoke
    Rake::Task['rb:docs'].invoke
    Rake::Task['dotnet:docs'].invoke
  end

  desc 'Build all artifacts for all language bindings'
  task :build, [:args] do |_task, arguments|
    args = arguments[:args] ? [arguments[:args]] : []
    Rake::Task['java:build'].invoke(args)
    Rake::Task['py:build'].invoke(args)
    Rake::Task['rb:build'].invoke(args)
    Rake::Task['dotnet:build'].invoke(args)
    Rake::Task['node:build'].invoke(args)
  end

  desc 'Release all artifacts for all language bindings'
  task :release, [:args] do |_task, arguments|
    Rake::Task['clean'].invoke
    args = arguments[:args] ? [arguments[:args]] : ['--stamp']
    Rake::Task['java:release'].invoke(args)
    Rake::Task['py:release'].invoke(args)
    Rake::Task['rb:release'].invoke(args)
    Rake::Task['dotnet:release'].invoke(args)
    Rake::Task['node:release'].invoke(args)
  end

  desc 'File updates for versions and metadata'
    task :update, [:channel] do |_task, arguments|
      args = arguments[:channel] ? ['--', "--chrome_channel=#{arguments[:channel].capitalize}"] : []
      Bazel.execute('run', args, '//scripts:update_cdp')
      Bazel.execute('run', args, '//scripts:pinned_browsers')
      Bazel.execute('run', args, '//scripts:selenium_manager')
      Rake::Task['java:dependencies'].invoke
      Rake::Task['authors'].invoke
      Rake::Task['copyright:update'].invoke
    end

  desc 'Update all versions'
  task :version, [:version] do |_task, arguments|
    version = arguments[:version]
    if version == 'nightly'
      Rake::Task['java:version'].invoke
      Rake::Task['rb:version'].invoke
    else
      Rake::Task['java:version'].invoke(version)
      Rake::Task['rb:version'].invoke(version)
      Rake::Task['node:version'].invoke(version)
      Rake::Task['py:version'].invoke(version)
      Rake::Task['dotnet:version'].invoke(version)
      Rake::Task['rust:version'].invoke(version)
    end
  end
end

at_exit do
  system 'sh', '.git-fixfiles' if File.exist?('.git') && !SeleniumRake::Checks.windows?
end

def updated_version(current, desired = nil)
  version = desired ? desired.split('.') : current.split(/\.|-/)
  if desired
    version << "0" while version.size < 3
  elsif version.size > 3
    version.pop while version.size > 3
  else
    version[1] = (version[1].to_i + 1).to_s
    version[2] = '0'
  end
  version.join('.')
end
